(function () {
    "use strict";

    const drawer = document.getElementById("venueDetailDrawer");
    const drawerOverlay = document.getElementById("venueDetailOverlay");
    const blockModal = document.getElementById("venueBlockModal");
    const blockOverlay = document.getElementById("venueBlockOverlay");
    const blockForm = document.getElementById("venueBlockForm");
    const blockReason = document.getElementById("venueBlockReason");
    const feedback = document.getElementById("venueFeedback");

    if (!drawer || !window.ManagementApi) {
        return;
    }

    let activeVenueId = null;
    let blockVenueId = null;
    let lastFocusedElement = null;

    function text(id, value) {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = value ?? "-";
        }
    }

    function displayValue(value) {
        return value === null || value === undefined || value === "" ? "-" : String(value);
    }

    function setStatusBadge(element, status) {
        if (!element) {
            return;
        }
        element.className = `venue-status-badge status-${String(status || "unknown").toLowerCase()}`;
        element.textContent = displayValue(status);
    }

    function showFeedback(message, type) {
        feedback.hidden = false;
        feedback.className = `alert ${type === "error" ? "alert-error" : "alert-success"}`;
        feedback.textContent = message;
        feedback.scrollIntoView({behavior: "smooth", block: "nearest"});
    }

    function syncBodyLock() {
        const dialogOpen = drawer.classList.contains("open") || blockModal.classList.contains("open");
        document.body.classList.toggle("venue-dialog-open", dialogOpen);
    }

    function openDrawer(trigger, venueId) {
        lastFocusedElement = trigger || document.activeElement;
        activeVenueId = venueId;
        drawer.classList.add("open");
        drawerOverlay.classList.add("visible");
        drawer.setAttribute("aria-hidden", "false");
        drawerOverlay.setAttribute("aria-hidden", "false");
        syncBodyLock();
        document.getElementById("venueDetailClose").focus();
        loadVenueDetail();
    }

    function closeDrawer() {
        drawer.classList.remove("open");
        drawerOverlay.classList.remove("visible");
        drawer.setAttribute("aria-hidden", "true");
        drawerOverlay.setAttribute("aria-hidden", "true");
        syncBodyLock();
        if (lastFocusedElement) {
            lastFocusedElement.focus();
        }
    }

    function setDetailState(state, message) {
        const loading = document.getElementById("venueDetailLoading");
        const error = document.getElementById("venueDetailError");
        const content = document.getElementById("venueDetailContent");
        loading.hidden = state !== "loading";
        error.hidden = state !== "error";
        content.hidden = state !== "content";
        if (message) {
            text("venueDetailErrorMessage", message);
        }
    }

    function renderAmenities(amenities) {
        const container = document.getElementById("detailAmenities");
        container.replaceChildren();
        text("detailAmenityCount", `${amenities.length} tiện ích`);

        if (!amenities.length) {
            const empty = document.createElement("p");
            empty.className = "venue-detail-empty";
            empty.textContent = "Venue chưa khai báo tiện ích.";
            container.appendChild(empty);
            return;
        }

        amenities.forEach(function (amenity) {
            const item = document.createElement("span");
            item.className = "venue-amenity-item";
            item.textContent = amenity.name;
            container.appendChild(item);
        });
    }

    function renderSpaces(spaces) {
        const container = document.getElementById("detailSpaces");
        container.replaceChildren();
        text("detailSpaceCount", `${spaces.length} space`);

        if (!spaces.length) {
            const empty = document.createElement("p");
            empty.className = "venue-detail-empty";
            empty.textContent = "Venue chưa có Space nào.";
            container.appendChild(empty);
            return;
        }

        spaces.forEach(function (space) {
            const card = document.createElement("article");
            card.className = "venue-space-card";

            const heading = document.createElement("div");
            heading.className = "venue-space-heading";
            const title = document.createElement("strong");
            title.textContent = displayValue(space.name);
            const status = document.createElement("span");
            status.textContent = displayValue(space.status);
            status.className = `space-status space-status-${String(space.status || "unknown").toLowerCase()}`;
            heading.append(title, status);

            const meta = document.createElement("div");
            meta.className = "venue-space-meta";
            const values = [
                space.type && `Loại: ${space.type}`,
                space.capacity != null && `Sức chứa: ${space.capacity}`,
                space.price != null && `Giá: ${space.price} ${space.priceUnit || ""}`.trim(),
                space.openTime && space.closeTime && `${space.openTime} – ${space.closeTime}`
            ].filter(Boolean);
            values.forEach(function (value) {
                const span = document.createElement("span");
                span.textContent = value;
                meta.appendChild(span);
            });

            card.append(heading, meta);
            if (space.description) {
                const description = document.createElement("p");
                description.textContent = space.description;
                card.appendChild(description);
            }
            container.appendChild(card);
        });
    }

    function renderDetail(detail) {
        text("detailVenueName", detail.name);
        text("detailVenueAddress", detail.address);
        text("detailVenueId", detail.id);
        text("detailVenueCity", detail.city);
        text("detailVenueStreet", detail.street);
        text("detailVenueCoordinates",
            detail.latitude != null && detail.longitude != null
                ? `${detail.latitude}, ${detail.longitude}`
                : "-");
        text("detailVenueDescription", detail.description);
        setStatusBadge(document.getElementById("detailVenueStatus"), detail.status);

        const reasonSection = document.getElementById("detailBlockReasonSection");
        reasonSection.hidden = detail.status !== "BLOCKED" || !detail.blockReason;
        text("detailBlockReason", detail.blockReason);

        const host = detail.host || {};
        text("detailHostName", host.name);
        text("detailHostEmail", host.email);
        text("detailHostPhone", host.phone);
        text("detailHostStatus", host.status);
        text("detailHostAvatar", (host.name || "H").trim().charAt(0).toUpperCase());
        text("detailHostIdentity", `CCCD: ${host.isIdentityVerified ? "Đã xác minh" : "Chưa xác minh"}`);
        text("detailHostBusiness", `Doanh nghiệp: ${host.isBusinessVerified ? "Đã xác minh" : "Chưa xác minh"}`);

        renderAmenities(Array.isArray(detail.amenities) ? detail.amenities : []);
        renderSpaces(Array.isArray(detail.spaces) ? detail.spaces : []);
        setDetailState("content");
    }

    async function loadVenueDetail() {
        if (!activeVenueId) {
            return;
        }
        setDetailState("loading");
        try {
            const detail = await ManagementApi.get(`/api/moderator/venues/${activeVenueId}`);
            renderDetail(detail);
        } catch (error) {
            setDetailState("error", error.message);
        }
    }

    function openBlockModal(trigger, venueId) {
        lastFocusedElement = trigger || document.activeElement;
        blockVenueId = venueId;
        blockReason.value = "";
        text("venueReasonCount", "0/500");
        text("venueBlockError", "");
        blockModal.classList.add("open");
        blockOverlay.classList.add("visible");
        blockModal.setAttribute("aria-hidden", "false");
        blockOverlay.setAttribute("aria-hidden", "false");
        syncBodyLock();
        blockReason.focus();
    }

    function closeBlockModal() {
        blockModal.classList.remove("open");
        blockOverlay.classList.remove("visible");
        blockModal.setAttribute("aria-hidden", "true");
        blockOverlay.setAttribute("aria-hidden", "true");
        blockVenueId = null;
        syncBodyLock();
        if (lastFocusedElement) {
            lastFocusedElement.focus();
        }
    }

    function updateRow(venueId, status) {
        setStatusBadge(document.getElementById(`venue-status-${venueId}`), status);
        const row = document.getElementById(`venue-row-${venueId}`);
        if (!row) {
            return;
        }
        const approveButton = row.querySelector(".venue-action-approve");
        const blockButton = row.querySelector(".venue-action-block");
        approveButton.hidden = status === "APPROVE";
        blockButton.hidden = status !== "APPROVE";
    }

    async function updateStatus(venueId, status, reason, button) {
        if (button) {
            button.disabled = true;
        }
        try {
            const result = await ManagementApi.put(`/api/moderator/venues/${venueId}/status`, {status, reason});
            updateRow(venueId, result.status);
            showFeedback(status === "BLOCKED" ? "Đã khóa Venue và ghi nhận lý do." : "Đã duyệt Venue thành công.", "success");
            if (activeVenueId === venueId && drawer.classList.contains("open")) {
                await loadVenueDetail();
            }
            return true;
        } catch (error) {
            showFeedback(error.message, "error");
            return false;
        } finally {
            if (button) {
                button.disabled = false;
            }
        }
    }

    document.querySelectorAll(".venue-detail-button").forEach(function (button) {
        button.addEventListener("click", function () {
            openDrawer(button, Number(button.dataset.venueId));
        });
    });

    document.querySelectorAll(".venue-action-approve").forEach(function (button) {
        button.addEventListener("click", function () {
            updateStatus(Number(button.dataset.venueId), "APPROVE", null, button);
        });
    });

    document.querySelectorAll(".venue-action-block").forEach(function (button) {
        button.addEventListener("click", function () {
            openBlockModal(button, Number(button.dataset.venueId));
        });
    });

    document.getElementById("venueDetailClose").addEventListener("click", closeDrawer);
    drawerOverlay.addEventListener("click", closeDrawer);
    document.getElementById("venueDetailRetry").addEventListener("click", loadVenueDetail);
    document.getElementById("venueBlockCancel").addEventListener("click", closeBlockModal);
    blockOverlay.addEventListener("click", closeBlockModal);

    blockReason.addEventListener("input", function () {
        text("venueReasonCount", `${blockReason.value.length}/500`);
        text("venueBlockError", "");
    });

    blockForm.addEventListener("submit", async function (event) {
        event.preventDefault();
        const reason = blockReason.value.trim();
        if (!reason) {
            text("venueBlockError", "Vui lòng nhập lý do khóa Venue.");
            blockReason.focus();
            return;
        }
        const submit = document.getElementById("venueBlockSubmit");
        submit.disabled = true;
        submit.textContent = "Đang khóa...";
        const success = await updateStatus(blockVenueId, "BLOCKED", reason, null);
        submit.disabled = false;
        submit.textContent = "Xác nhận khóa";
        if (success) {
            closeBlockModal();
        }
    });

    document.addEventListener("keydown", function (event) {
        if (event.key !== "Escape") {
            return;
        }
        if (blockModal.classList.contains("open")) {
            closeBlockModal();
        } else if (drawer.classList.contains("open")) {
            closeDrawer();
        }
    });
})();
