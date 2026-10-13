const $ = (id) => document.getElementById(id);

let jwt = sessionStorage.getItem("careplus_jwt") || "";

$("baseUrl").value = sessionStorage.getItem("careplus_base_url") || "";

/**
 * Returns the configured Facade URL.
 * If the UI is served by the Facade itself, use the current origin.
 */
function getBaseUrl() {
    const configuredUrl = $("baseUrl").value.trim().replace(/\/+$/, "");
    return configuredUrl || window.location.origin;
}

function updateAuthStatus() {
    $("authStatus").textContent = jwt ? "Authenticated" : "Please sign in";
}

function setSignedIn(isSignedIn) {
    $("loginScreen").classList.toggle("hidden", isSignedIn);
    $("appShell").classList.toggle("hidden", !isSignedIn);
    updateAuthStatus();
}

function showMessage(message, isError = false) {
    const messageElement = $("message");

    messageElement.textContent = message;
    messageElement.className = `notice ${isError ? "error" : "ok"}`;

    window.scrollTo({ top: 0, behavior: "smooth" });
}

function showResult(elementId, data) {
    const element = $(elementId);

    element.classList.remove("hidden");
    element.textContent =
        typeof data === "string" ? data : JSON.stringify(data, null, 2);
}

/**
 * Calls the Facade API.
 * Authenticated requests automatically include the JWT bearer token.
 */
async function apiRequest(path, options = {}) {
    const baseUrl = getBaseUrl();
    const isLoginRequest = path === API_URLS.AUTH.LOGIN;

    sessionStorage.setItem("careplus_base_url", $("baseUrl").value.trim());

    if (!isLoginRequest && !jwt) {
        throw new Error("Please log in first.");
    }

    const headers = {
        "Content-Type": "application/json",
        ...(options.headers || {}),
    };

    if (jwt && !isLoginRequest) {
        headers.Authorization = `Bearer ${jwt}`;
    }

    const response = await fetch(`${baseUrl}${path}`, {
        ...options,
        headers,
    });

    const responseText = await response.text();
    let responseData = null;

    try {
        responseData = responseText ? JSON.parse(responseText) : null;
    } catch {
        responseData = responseText;
    }

    if (!response.ok) {
        if (response.status === 401 || response.status === 403) {
            jwt = "";
            sessionStorage.removeItem("careplus_jwt");
            setSignedIn(false);
        }

        const details =
            typeof responseData === "string"
                ? responseData
                : JSON.stringify(responseData);

        throw new Error(
            `HTTP ${response.status} ${response.statusText}\n${details}`
        );
    }

    return responseData;
}

function postJson(data) {
    return {
        method: "POST",
        body: JSON.stringify(data),
    };
}

function getRecordId(data) {
    return (
        data?.id ??
        data?.patientId ??
        data?.doctorId ??
        data?.appointmentId ??
        ""
    );
}

/* -------------------------------------------------------------------------- */
/* Authentication                                                             */
/* -------------------------------------------------------------------------- */

$("loginForm").addEventListener("submit", async (event) => {
    event.preventDefault();

    try {
        const response = await apiRequest(
            API_URLS.AUTH.LOGIN,
            postJson({
                username: $("username").value.trim(),
                password: $("password").value,
            })
        );

        jwt =
            response?.token ||
            response?.accessToken ||
            response?.jwt ||
            response?.access_token ||
            "";

        if (!jwt) {
            throw new Error(
                "The login response did not contain token, accessToken, jwt, " +
                "or access_token. Update app.js to match your login response DTO."
            );
        }

        sessionStorage.setItem("careplus_jwt", jwt);
        $("password").value = "";
        $("welcomeUser").textContent =
            `Signed in as ${$("username").value.trim()}`;

        setSignedIn(true);

        showMessage(
            "Login successful. Subsequent API requests will include the JWT " +
            "in the Authorization: Bearer header."
        );
    } catch (error) {
        showMessage(`Login failed: ${error.message}`, true);
    }
});

$("logoutBtn").addEventListener("click", () => {
    jwt = "";
    sessionStorage.removeItem("careplus_jwt");
    $("password").value = "";

    setSignedIn(false);
    showMessage("You have been logged out.");
});

if (jwt) {
    setSignedIn(true);
    $("welcomeUser").textContent = "Signed in (restored for this tab)";
} else {
    setSignedIn(false);
    signInWithGoogleSession();
}

/**
 * After Google sign-in, exchanges the Google session for a CarePlus JWT. When the Google email matches a
 * user in the database the user is signed in directly, with that user's username and role; otherwise the
 * username/password screen stays visible.
 */
async function signInWithGoogleSession() {
    try {
        const response = await fetch(`${getBaseUrl()}${API_URLS.AUTH.GOOGLE_TOKEN}`);
        if (!response.ok) {
            if (response.status === 404) {
                showMessage("Your Google account is not registered in CarePlus. Sign in with username and password.", true);
            }
            return;
        }
        const login = await response.json();
        if (!login?.token) {
            return;
        }
        jwt = login.token;
        sessionStorage.setItem("careplus_jwt", jwt);
        $("welcomeUser").textContent = `Signed in as ${login.username} (${login.role})`;
        setSignedIn(true);
        showMessage("Signed in with Google.");
    } catch {
        // No Google session available; the username/password screen stays visible.
    }
}

/* -------------------------------------------------------------------------- */
/* Navigation                                                                 */
/* -------------------------------------------------------------------------- */

document.querySelectorAll(".tab").forEach((button) => {
    button.addEventListener("click", () => {
        document.querySelectorAll(".tab").forEach((tab) => {
            tab.classList.toggle("active", tab === button);
        });

        document.querySelectorAll(".tabPanel").forEach((panel) => {
            panel.classList.toggle("hidden", panel.id !== button.dataset.tab);
        });
    });
});

/* -------------------------------------------------------------------------- */
/* Patients                                                                   */
/* -------------------------------------------------------------------------- */

$("resetPatient").addEventListener("click", () => {
    $("patientForm").reset();
    $("patientId").value = "";
    $("patientFormTitle").textContent = "Register patient";
});

$("patientForm").addEventListener("submit", async (event) => {
    event.preventDefault();

    const patientId = $("patientId").value;

    const patient = {
        name: $("patientName").value.trim(),
        email: $("patientEmail").value.trim(),
        phone: $("patientPhone").value.trim(),
        date_of_birth: $("patientDob").value || null,
        gender: $("patientGender").value,
        address: $("patientAddress").value.trim(),
    };

    const path = patientId
        ? API_URLS.PATIENTS.BY_ID(patientId)
        : API_URLS.PATIENTS.BASE;

    try {
        const response = await apiRequest(path, {
            method: patientId ? "PUT" : "POST",
            body: JSON.stringify(patient),
        });

        showMessage(`Patient saved: ${JSON.stringify(response)}`);

        if (getRecordId(response)) {
            $("patientId").value = getRecordId(response);
        }
    } catch (error) {
        showMessage(error.message, true);
    }
});

$("findPatientBtn").addEventListener("click", async () => {
    const patientId = encodeURIComponent($("patientLookupId").value.trim());

    try {
        const patient = await apiRequest(API_URLS.PATIENTS.BY_ID(patientId));

        showResult("patientLookupResult", patient);

        $("patientId").value = getRecordId(patient);
        $("patientName").value = patient.name || patient.fullName || "";
        $("patientEmail").value = patient.email || "";
        $("patientPhone").value = patient.phone || "";
        $("patientDob").value = patient.dateOfBirth || "";
        $("patientGender").value = patient.gender || "";
        $("patientAddress").value = patient.address || "";
        $("patientFormTitle").textContent = "Edit patient";
    } catch (error) {
        showMessage(error.message, true);
    }
});

/* -------------------------------------------------------------------------- */
/* Doctors                                                                    */
/* -------------------------------------------------------------------------- */

$("resetDoctor").addEventListener("click", () => {
    $("doctorForm").reset();
    $("doctorId").value = "";
    $("doctorFormTitle").textContent = "Register doctor";
});

$("doctorForm").addEventListener("submit", async (event) => {
    event.preventDefault();

    const doctorId = $("doctorId").value;

    const doctor = {
        name: $("doctorName").value.trim(),
        specialization: $("doctorSpecialty").value.trim(),
        email: $("doctorEmail").value.trim(),
        phone: $("doctorPhone").value.trim(),
	    active: $("doctorActive").value === "true"
    };
	

    const path = doctorId
        ? API_URLS.DOCTORS.BY_ID(doctorId)
        : API_URLS.DOCTORS.BASE;

    try {
        const response = await apiRequest(path, {
            method: doctorId ? "PUT" : "POST",
            body: JSON.stringify(doctor),
        });

        showMessage(`Doctor saved: ${JSON.stringify(response)}`);

        if (getRecordId(response)) {
            $("doctorId").value = getRecordId(response);
        }
    } catch (error) {
        showMessage(error.message, true);
    }
});

$("findDoctorBtn").addEventListener("click", async () => {
    const doctorId = encodeURIComponent($("doctorLookupId").value.trim());

    try {
        const doctor = await apiRequest(API_URLS.DOCTORS.BY_ID(doctorId));

        showResult("doctorLookupResult", doctor);

        $("doctorId").value = getRecordId(doctor);
        $("doctorName").value = doctor.name || doctor.fullName || "";
        $("doctorSpecialty").value = doctor.specialty || "";
        $("doctorEmail").value = doctor.email || "";
        $("doctorPhone").value = doctor.phone || "";
        $("doctorFormTitle").textContent = "Edit doctor";
    } catch (error) {
        showMessage(error.message, true);
    }
});

$("availabilityBtn").addEventListener("click", async () => {
    const doctorId = encodeURIComponent(
        $("availabilityDoctorId").value.trim()
    );

    try {
        const availability = await apiRequest(
            API_URLS.DOCTORS.AVAILABILITY(doctorId)
        );

        showResult("availabilityResult", availability);
    } catch (error) {
        showMessage(error.message, true);
    }
});

/* -------------------------------------------------------------------------- */
/* Appointments                                                               */
/* -------------------------------------------------------------------------- */

$("appointmentForm").addEventListener("submit", async (event) => {
    event.preventDefault();

    const appointment = {
		patient_id: Number($("appointmentPatientId").value.trim()),
		doctor_id: Number($("appointmentDoctorId").value.trim()),
		appointmentDate: $("appointmentDate").value,
		appointmentTime: $("appointmentTime").value,
		reason: $("appointmentReason").value.trim(),
	    status: $("appointmentStatus").value,
    };

    try {
        const response = await apiRequest(
            API_URLS.APPOINTMENTS.BASE,
            postJson(appointment)
        );

        showResult("appointmentResult", response);
        showMessage(`Booking response: ${JSON.stringify(response)}`);

        if (getRecordId(response)) {
            $("manageAppointmentId").value = getRecordId(response);
        }
    } catch (error) {
        showMessage(error.message, true);
    }
});

$("getAppointmentBtn").addEventListener("click", async () => {
    const appointmentId = encodeURIComponent(
        $("manageAppointmentId").value.trim()
    );

    try {
        const appointment = await apiRequest(
            API_URLS.APPOINTMENTS.BY_ID(appointmentId)
        );

        showResult("appointmentResult", appointment);
    } catch (error) {
        showMessage(error.message, true);
    }
});

$("cancelAppointmentBtn").addEventListener("click", async () => {
    const appointmentId = encodeURIComponent(
        $("manageAppointmentId").value.trim()
    );

    try {
        const response = await apiRequest(
            API_URLS.APPOINTMENTS.CANCEL(appointmentId),
            { method: "PUT" }
        );

        showResult(
            "appointmentResult",
            response ?? "Appointment cancelled"
        );
        showMessage("Appointment cancelled.");
    } catch (error) {
        showMessage(error.message, true);
    }
});

$("completeAppointmentBtn").addEventListener("click", async () => {
    const appointmentId = encodeURIComponent(
        $("manageAppointmentId").value.trim()
    );

    try {
        const response = await apiRequest(
            API_URLS.APPOINTMENTS.COMPLETE(appointmentId),
            { method: "PUT" }
        );

        showResult(
            "appointmentResult",
            response ?? "Appointment completed"
        );
        showMessage("Appointment completed.");
    } catch (error) {
        showMessage(error.message, true);
    }
});

function renderTable(containerId, data) {
    const container = $(containerId);
    container.innerHTML = "";

    const rows = Array.isArray(data)
        ? data
        : Array.isArray(data?.content)
            ? data.content
            : null;

    if (!rows) {
        container.textContent = JSON.stringify(data, null, 2);
        return;
    }

    if (rows.length === 0) {
        container.textContent = "No records returned.";
        return;
    }

    const columns = [
        ...new Set(rows.flatMap((row) => Object.keys(row || {}))),
    ].slice(0, 8);

    const table = document.createElement("table");
    const header = document.createElement("thead");
    const headerRow = document.createElement("tr");

    columns.forEach((column) => {
        const cell = document.createElement("th");
        cell.textContent = column;
        headerRow.appendChild(cell);
    });

    header.appendChild(headerRow);
    table.appendChild(header);

    const body = document.createElement("tbody");

    rows.forEach((row) => {
        const tableRow = document.createElement("tr");

        columns.forEach((column) => {
            const cell = document.createElement("td");
            const value = row[column];

            cell.textContent =
                typeof value === "object"
                    ? JSON.stringify(value)
                    : (value ?? "");

            tableRow.appendChild(cell);
        });

        body.appendChild(tableRow);
    });

    table.appendChild(body);
    container.appendChild(table);
}

$("patientAppointmentsBtn").addEventListener("click", async () => {
    const patientId = encodeURIComponent(
        $("appointmentsPatientId").value.trim()
    );

    try {
        const appointments = await apiRequest(
            API_URLS.PATIENTS.BY_PATIENT(patientId)
        );

        renderTable("appointmentsList", appointments);
    } catch (error) {
        showMessage(error.message, true);
    }
});

$("doctorAppointmentsBtn").addEventListener("click", async () => {
    const doctorId = encodeURIComponent(
        $("appointmentsDoctorId").value.trim()
    );

    try {
        const appointments = await apiRequest(
            API_URLS.DOCTORS.BY_DOCTOR(doctorId)
        );

        renderTable("appointmentsList", appointments);
    } catch (error) {
        showMessage(error.message, true);
    }
});

/* -------------------------------------------------------------------------- */
/* Medical history                                                            */
/* -------------------------------------------------------------------------- */

$("historyBtn").addEventListener("click", async () => {
    const patientId = encodeURIComponent(
        $("historyPatientId").value.trim()
    );

    try {
        const history = await apiRequest(
            API_URLS.PATIENTS.MEDICAL_HISTORY(patientId)
        );

        showResult("historyResult", history);
    } catch (error) {
        showMessage(error.message, true);
    }
});
