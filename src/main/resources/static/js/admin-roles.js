(function () {
    "use strict";

    if (!window.ManagementApi) {
        return;
    }

    const state = {page: 0, size: 10, totalPages: 0, last: true};
    const tableBody = document.getElementById("roleTableBody");
    const tableWrap = document.getElementById("roleTableWrap");
    const loading = document.getElementById("roleLoading");
    const errorState = document.getElementById("roleError");
    const emptyState = document.getElementById("roleEmpty");
    const pagination = document.getElementById("rolePagination");
    const feedback = document.getElementById("roleFeedback");

    function setVisible(element, visible) {
        element.hidden = !visible;
    }

    function showFeedback(message, type) {
        feedback.hidden = false;
        feedback.className = `alert ${type === "error" ? "alert-error" : "alert-success"}`;
        feedback.textContent = message;
    }

    function buildQuery() {
        const params = new URLSearchParams({page: state.page, size: state.size});
        const keyword = document.getElementById("roleKeyword").value.trim();
        const status = document.getElementById("roleStatus").value;
        const role = document.getElementById("roleCurrent").value;
        if (keyword) params.set("keyword", keyword);
        if (status) params.set("status", status);
        if (role) params.set("role", role);
        return params.toString();
    }

    function createRoleChip(role) {
        const chip = document.createElement("span");
        chip.className = "role-chip";
        chip.textContent = role;
        return chip;
    }

    function renderUserRow(user) {
        const row = document.createElement("tr");

        const userCell = document.createElement("td");
        const userBox = document.createElement("div");
        userBox.className = "role-user";
        const name = document.createElement("strong");
        name.textContent = user.name || "Chưa cập nhật tên";
        const email = document.createElement("span");
        email.textContent = user.email || "-";
        userBox.append(name, email);
        userCell.appendChild(userBox);

        const statusCell = document.createElement("td");
        const status = document.createElement("span");
        status.className = `role-status role-status-${String(user.status || "unknown").toLowerCase()}`;
        status.textContent = user.status || "UNKNOWN";
        statusCell.appendChild(status);

        const roles = Array.from(user.roles || []).sort();
        const rolesCell = document.createElement("td");
        roles.forEach(function (role) { rolesCell.appendChild(createRoleChip(role)); });

        const changeCell = document.createElement("td");
        const changeForm = document.createElement("form");
        changeForm.className = "role-inline-form";
        const select = document.createElement("select");
        select.setAttribute("aria-label", `Role mới cho ${user.name || user.email}`);
        const placeholder = document.createElement("option");
        placeholder.value = "";
        placeholder.textContent = "Chọn role";
        placeholder.disabled = true;
        placeholder.selected = true;
        select.appendChild(placeholder);
        ["USER", "HOST", "MODERATOR"].forEach(function (role) {
            const option = document.createElement("option");
            option.value = role;
            option.textContent = role;
            const currentElevated = roles.find(function (value) {
                return ["HOST", "MODERATOR", "ADMIN"].includes(value);
            }) || "USER";
            option.disabled = role === currentElevated;
            select.appendChild(option);
        });
        const submit = document.createElement("button");
        submit.type = "submit";
        submit.textContent = "Cập nhật";
        changeForm.append(select, submit);
        changeForm.addEventListener("submit", async function (event) {
            event.preventDefault();
            if (!select.value) return;
            submit.disabled = true;
            try {
                await ManagementApi.put(`/api/admin/users/${user.id}/role`, {role: select.value});
                showFeedback(`Đã đổi role của ${user.email} sang ${select.value}.`, "success");
                await loadUsers();
            } catch (requestError) {
                showFeedback(requestError.message, "error");
            } finally {
                submit.disabled = false;
            }
        });
        changeCell.appendChild(changeForm);

        const removeCell = document.createElement("td");
        const removeList = document.createElement("div");
        removeList.className = "role-remove-list";
        roles.filter(function (role) { return role !== "USER"; }).forEach(function (role) {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "role-remove-button";
            button.textContent = `Gỡ ${role}`;
            button.addEventListener("click", async function () {
                if (!window.confirm(`Gỡ role ${role} khỏi ${user.email}?`)) return;
                button.disabled = true;
                try {
                    await ManagementApi.delete(`/api/admin/users/${user.id}/roles/${encodeURIComponent(role)}`);
                    showFeedback(`Đã gỡ role ${role} khỏi ${user.email}.`, "success");
                    await loadUsers();
                } catch (requestError) {
                    showFeedback(requestError.message, "error");
                } finally {
                    button.disabled = false;
                }
            });
            removeList.appendChild(button);
        });
        if (!removeList.childElementCount) {
            const noRole = document.createElement("span");
            noRole.textContent = "Không có role nâng cao";
            noRole.className = "role-user";
            removeList.appendChild(noRole);
        }
        removeCell.appendChild(removeList);

        row.append(userCell, statusCell, rolesCell, changeCell, removeCell);
        return row;
    }

    function renderPage(page) {
        const users = Array.isArray(page.content) ? page.content : [];
        tableBody.replaceChildren();
        users.forEach(function (user) { tableBody.appendChild(renderUserRow(user)); });

        state.totalPages = page.totalPages || 0;
        state.last = Boolean(page.last);
        setVisible(loading, false);
        setVisible(errorState, false);
        setVisible(emptyState, users.length === 0);
        setVisible(tableWrap, users.length > 0);
        setVisible(pagination, users.length > 0 && state.totalPages > 1);
        document.getElementById("rolePageInfo").textContent = `Trang ${page.pageNumber + 1} / ${Math.max(1, page.totalPages)}`;
        document.getElementById("rolePrevious").disabled = page.pageNumber <= 0;
        document.getElementById("roleNext").disabled = page.last;
    }

    async function loadUsers() {
        setVisible(loading, true);
        setVisible(errorState, false);
        setVisible(emptyState, false);
        setVisible(tableWrap, false);
        setVisible(pagination, false);
        try {
            const page = await ManagementApi.get(`/api/moderator/users?${buildQuery()}`);
            renderPage(page);
        } catch (requestError) {
            setVisible(loading, false);
            setVisible(errorState, true);
            document.getElementById("roleErrorMessage").textContent = requestError.message;
        }
    }

    document.getElementById("roleFilterForm").addEventListener("submit", function (event) {
        event.preventDefault();
        state.page = 0;
        loadUsers();
    });
    document.getElementById("roleRetry").addEventListener("click", loadUsers);
    document.getElementById("rolePrevious").addEventListener("click", function () {
        if (state.page > 0) { state.page -= 1; loadUsers(); }
    });
    document.getElementById("roleNext").addEventListener("click", function () {
        if (!state.last) { state.page += 1; loadUsers(); }
    });

    loadUsers();
})();
