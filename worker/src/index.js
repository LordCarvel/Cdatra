function errorResponse (status, code, message) {

    const body = {

        ok: false,
        error: {
            code: code,
            message: message
        }
    };

    return Response.json(body, { status: status });
}

function successResponse (result) {

    const body = {

        ok: true,
        result: result
    };

    return Response.json(body);
}

async function sha256 (text) {

    const bytes = new TextEncoder().encode(text);

    return crypto.subtle.digest("SHA-256", bytes);
}

async function isAuthorized (request, secret) {

    const header = request.headers.get("Authorization");
    const secretValue = "Bearer " + secret;

    const headerHash = await sha256(header);
    const secretHash = await sha256(secretValue);

    return crypto.subtle.timingSafeEqual(headerHash, secretHash);
}

async function readBody (request) {

    try {

        return await request.json();
    } catch {

        return null;
    }
}

export default {

    async fetch (request, env) {

        if (!env.CDATRA_SECRET) {

            return errorResponse(500, "INTERNAL", "Worker secret is not configured");
        }

        const authorized = await isAuthorized(request, env.CDATRA_SECRET);

        if (!authorized) {

            return errorResponse(401, "UNAUTHORIZED", "Invalid or missing secret");
        }

        return errorResponse(404, "NOT_FOUND", "Route not found");
    }
};