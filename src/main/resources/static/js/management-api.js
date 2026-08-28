(function (window) {
    "use strict";

    function getAccessToken() {
        return sessionStorage.getItem("accessToken")
            || localStorage.getItem("accessToken");
    }

    function clearAuthentication() {
        sessionStorage.removeItem("accessToken");
        sessionStorage.removeItem("refreshToken");
        sessionStorage.removeItem("currentUser");
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
    }

    async function request(url, options) {
        const requestOptions = Object.assign({credentials: "same-origin"}, options || {});
        const headers = new Headers(requestOptions.headers || {});
        const token = getAccessToken();

        if (token) {
            headers.set("Authorization", `Bearer ${token}`);
        }
        if (requestOptions.body && !headers.has("Content-Type")) {
            headers.set("Content-Type", "application/json");
        }

        requestOptions.headers = headers;
        const response = await fetch(url, requestOptions);
        let payload = null;

        try {
            payload = await response.json();
        } catch (error) {
            payload = null;
        }

        if (response.status === 401) {
            clearAuthentication();
            window.location.replace("/login");
            throw new Error("Phiên đăng nhập đã hết hạn.");
        }
        if (!response.ok) {
            const requestError = new Error(payload?.message || "Không thể xử lý yêu cầu.");
            requestError.status = response.status;
            requestError.payload = payload;
            throw requestError;
        }

        return payload?.data;
    }

    window.ManagementApi = {
        get: function (url) {
            return request(url, {method: "GET"});
        },
        put: function (url, body) {
            return request(url, {method: "PUT", body: JSON.stringify(body)});
        },
        delete: function (url) {
            return request(url, {method: "DELETE"});
        }
    };
})(window);
