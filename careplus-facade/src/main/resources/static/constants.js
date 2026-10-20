/**
 * Centralized CarePlus Facade API endpoints.
 * All paths are relative to the Facade base URL selected in the UI.
 * Dynamic segments are URL-encoded here, not in app.js.
 */
const API_URLS = Object.freeze({
    AUTH: Object.freeze({
        LOGIN: "/api/auth/login",
        GOOGLE_TOKEN: "/api/auth/google-token",
    }),
    PATIENTS: Object.freeze({
        BASE: "/api/patients/create",
        BY_ID: (id) => `/api/patients/${encodeURIComponent(id)}`,
        MEDICAL_HISTORY: (id) => `/api/patients/${encodeURIComponent(id)}/medical-history`,
        MEDICAL_RECORDS: "/api/patients/medical-records",
		BY_PATIENT: (id) => `/api/patients/${encodeURIComponent(id)}/appointments`,
    }),
    DOCTORS: Object.freeze({
        BASE: "/api/doctors/create",
        BY_ID: (id) => `/api/doctors/${encodeURIComponent(id)}`,
        AVAILABILITY: (id) => `/api/doctors/${encodeURIComponent(id)}/availability`,
	    BY_DOCTOR: (id) => `/api/doctors/${encodeURIComponent(id)}/appointments`,
    }),
    APPOINTMENTS: Object.freeze({
        BASE: "/api/appointments",
        BY_ID: (id) => `/api/appointments/${encodeURIComponent(id)}`,
        CANCEL: (id) => `/api/appointments/${encodeURIComponent(id)}/cancel`,
        COMPLETE: (id) => `/api/appointments/${encodeURIComponent(id)}/complete`,
    }),
});
