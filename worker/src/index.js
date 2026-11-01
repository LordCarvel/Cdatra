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

export default {

    fetch (request, env) {

        return errorResponse(404, "NOT_FOUND", "Route not found");
    }
};