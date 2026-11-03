import assert from "node:assert/strict";
import { beforeEach, describe, it } from "node:test";

import worker from "../src/index.js";

const SECRET = "test-secret";

crypto.subtle.timingSafeEqual = (first, second) => Buffer.from(first).equals(Buffer.from(second));

function fakeDatabase (answers = {}) {

    const calls = { sql: [], bound: [], raw: [], run: 0 };

    const statement = {

        async raw (options) {

            calls.raw.push(options);

            if (answers.raw instanceof Error) {

                throw answers.raw;
            }

            return answers.raw ?? [];
        },

        async run () {

            calls.run = calls.run + 1;

            if (answers.run instanceof Error) {

                throw answers.run;
            }

            return answers.run ?? { results: [], meta: { last_row_id: 0, changes: 0 } };
        }
    };

    return {

        calls,

        prepare (sql) {

            calls.sql.push(sql);

            return {

                bind (...values) {

                    calls.bound.push(values);

                    if (answers.bind instanceof Error) {

                        throw answers.bind;
                    }

                    return statement;
                }
            };
        }
    };
}

function request (path, body, headers = { Authorization: "Bearer " + SECRET }) {

    return new Request("https://worker.test" + path, { method: "POST", headers: headers, body: body });
}

function json (sql, params) {

    return JSON.stringify({ sql: sql, params: params });
}

async function assertError (response, status, code) {

    assert.equal(response.status, status);

    const body = await response.json();

    assert.equal(body.ok, false);
    assert.equal(body.error.code, code);
    assert.equal(typeof body.error.message, "string");

    return body;
}

describe("secret", () => {

    it("refuses everything when the secret is not configured", async () => {

        const db = fakeDatabase();

        await assertError(await worker.fetch(request("/v1/query", json("SELECT 1", [])), { DB: db }), 500, "INTERNAL");
        assert.deepEqual(db.calls.sql, []);
    });

    it("refuses a wrong secret", async () => {

        const db = fakeDatabase();
        const response = await worker.fetch(request("/v1/query", json("SELECT 1", []), { Authorization: "Bearer wrong" }), { CDATRA_SECRET: SECRET, DB: db });

        await assertError(response, 401, "UNAUTHORIZED");
        assert.deepEqual(db.calls.sql, []);
    });

    it("refuses a missing header", async () => {

        const response = await worker.fetch(request("/v1/query", json("SELECT 1", []), {}), { CDATRA_SECRET: SECRET, DB: fakeDatabase() });

        await assertError(response, 401, "UNAUTHORIZED");
    });

    it("refuses the secret without the Bearer prefix", async () => {

        const response = await worker.fetch(request("/v1/query", json("SELECT 1", []), { Authorization: SECRET }), { CDATRA_SECRET: SECRET, DB: fakeDatabase() });

        await assertError(response, 401, "UNAUTHORIZED");
    });

    it("checks the secret before the route", async () => {

        const response = await worker.fetch(request("/v1/unknown", "", { Authorization: "Bearer wrong" }), { CDATRA_SECRET: SECRET, DB: fakeDatabase() });

        await assertError(response, 401, "UNAUTHORIZED");
    });
});

describe("requests", () => {

    let db;
    let env;

    beforeEach(() => {

        db = fakeDatabase();
        env = { CDATRA_SECRET: SECRET, DB: db };
    });

    it("answers 404 for an unknown route", async () => {

        await assertError(await worker.fetch(request("/v1/unknown", json("SELECT 1", [])), env), 404, "NOT_FOUND");
    });

    it("checks the route before the body", async () => {

        await assertError(await worker.fetch(request("/v1/unknown", "not json"), env), 404, "NOT_FOUND");
    });

    const invalidBodies = {

        "broken JSON": "not json",
        "empty text": "",
        "JSON null": "null",
        "a number": "5",
        "a list": "[]",
        "no sql": JSON.stringify({ params: [] }),
        "no params": JSON.stringify({ sql: "SELECT 1" }),
        "sql that is not text": JSON.stringify({ sql: 1, params: [] }),
        "params that are not a list": JSON.stringify({ sql: "SELECT 1", params: {} })
    };

    for (const route of ["/v1/query", "/v1/execute"]) {

        for (const [name, body] of Object.entries(invalidBodies)) {

            it("answers 400 on " + route + " for " + name, async () => {

                await assertError(await worker.fetch(request(route, body), env), 400, "BAD_REQUEST");
                assert.deepEqual(db.calls.sql, []);
            });
        }
    }

    it("accepts unknown body fields", async () => {

        const response = await worker.fetch(request("/v1/query", JSON.stringify({ sql: "SELECT 1", params: [], extra: true })), env);

        assert.equal(response.status, 200);
    });
});

describe("/v1/query", () => {

    it("separates columns from rows", async () => {

        const db = fakeDatabase({ raw: [["id", "name"], [1, "Ana"], [2, "Bia"]] });
        const response = await worker.fetch(request("/v1/query", json("SELECT id, name FROM users", [])), { CDATRA_SECRET: SECRET, DB: db });

        assert.equal(response.status, 200);
        assert.deepEqual(await response.json(), {

            ok: true,
            result: { columns: ["id", "name"], rows: [[1, "Ana"], [2, "Bia"]], meta: {} }
        });
    });

    it("runs the statement once, with raw and column names", async () => {

        const db = fakeDatabase({ raw: [["id"]] });

        await worker.fetch(request("/v1/query", json("SELECT id FROM users", [])), { CDATRA_SECRET: SECRET, DB: db });

        assert.deepEqual(db.calls.raw, [{ columnNames: true }]);
        assert.equal(db.calls.run, 0);
    });

    it("keeps the columns of an empty result", async () => {

        const db = fakeDatabase({ raw: [["id", "name"]] });
        const response = await worker.fetch(request("/v1/query", json("SELECT id, name FROM users", [])), { CDATRA_SECRET: SECRET, DB: db });

        assert.deepEqual((await response.json()).result, { columns: ["id", "name"], rows: [], meta: {} });
    });

    it("accepts a completely empty result", async () => {

        const db = fakeDatabase({ raw: [] });
        const response = await worker.fetch(request("/v1/query", json("SELECT id FROM users", [])), { CDATRA_SECRET: SECRET, DB: db });

        assert.deepEqual((await response.json()).result, { columns: [], rows: [], meta: {} });
    });

    it("sends each parameter as its own argument", async () => {

        const db = fakeDatabase();

        await worker.fetch(request("/v1/query", json("SELECT * FROM users WHERE id = ? AND name = ? AND email = ?", [1, "Ana", null])), { CDATRA_SECRET: SECRET, DB: db });

        assert.deepEqual(db.calls.sql, ["SELECT * FROM users WHERE id = ? AND name = ? AND email = ?"]);
        assert.deepEqual(db.calls.bound, [[1, "Ana", null]]);
    });

    it("accepts a statement without parameters", async () => {

        const db = fakeDatabase();

        await worker.fetch(request("/v1/query", json("SELECT 1", [])), { CDATRA_SECRET: SECRET, DB: db });

        assert.deepEqual(db.calls.bound, [[]]);
    });
});

describe("/v1/execute", () => {

    it("answers only with last_row_id", async () => {

        const db = fakeDatabase({ run: { results: [], meta: { last_row_id: 42, changes: 1, duration: 0.1 } } });
        const response = await worker.fetch(request("/v1/execute", json("INSERT INTO users (name) VALUES (?)", ["Ana"])), { CDATRA_SECRET: SECRET, DB: db });

        assert.equal(response.status, 200);
        assert.deepEqual(await response.json(), {

            ok: true,
            result: { columns: [], rows: [], meta: { last_row_id: 42 } }
        });
    });

    it("runs the statement once, with run", async () => {

        const db = fakeDatabase();

        await worker.fetch(request("/v1/execute", json("DELETE FROM users WHERE id = ?", [1])), { CDATRA_SECRET: SECRET, DB: db });

        assert.equal(db.calls.run, 1);
        assert.deepEqual(db.calls.raw, []);
        assert.deepEqual(db.calls.bound, [[1]]);
    });
});

describe("D1 errors", () => {

    async function assertSqlError (path, answers, text) {

        const db = fakeDatabase(answers);
        const response = await worker.fetch(request(path, json("SELECT 1", [])), { CDATRA_SECRET: SECRET, DB: db });
        const body = await assertError(response, 400, "SQL_ERROR");

        assert.equal(body.error.message, text);
    }

    it("answers SQL_ERROR when a read fails", async () => {

        await assertSqlError("/v1/query", { raw: new Error("D1_ERROR: no such table: users") }, "D1_ERROR: no such table: users");
    });

    it("answers SQL_ERROR when a write fails", async () => {

        await assertSqlError("/v1/execute", { run: new Error("D1_ERROR: UNIQUE constraint failed: users.name") }, "D1_ERROR: UNIQUE constraint failed: users.name");
    });

    it("answers SQL_ERROR when bind fails", async () => {

        await assertSqlError("/v1/query", { bind: new Error("D1_TYPE_ERROR: Type 'object' not supported") }, "D1_TYPE_ERROR: Type 'object' not supported");
    });
});
