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

export default {

    fetch (request, env) {

        if (!env.CDATRA_SECRET) {

            return errorResponse(500, "INTERNAL", "Worker secret is not configured");
        }

        return errorResponse(404, "NOT_FOUND", "Route not found");
    }
};