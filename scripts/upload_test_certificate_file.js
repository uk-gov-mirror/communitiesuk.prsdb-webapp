// Test Certificate File Uploader
//
// Used for uploading the EICAR string for virus scanning without the file existing on users machine.
//
// Usage:
//   1. Log in and navigate to a gas or electrical safety certificate upload page
//   2. Open the browser console (F12 → Console)
//   3. Paste this entire script and press Enter
//      (if prompted to, type "allow pasting" and press Enter first)
//   4. Enter the file content string when prompted
//   5. Click "Continue" on the page as normal to submit the upload

const TEST_FILE_NAME = "test-certificate.pdf";
const TEST_FILE_MIME_TYPE = "application/pdf";

function uploadTestCertificateFile(content) {
    const fileInput = document.querySelector(
        'form#single-file-upload-form input.govuk-file-upload'
    );

    if (!fileInput) {
        console.error(
            "Did not locate the certificate upload input. " +
            "This script only works on gas/electrical safety certificate upload pages."
        );
        return;
    }

    const file = new File([content], TEST_FILE_NAME, {
        type: TEST_FILE_MIME_TYPE,
    });

    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(file);
    fileInput.files = dataTransfer.files;

    fileInput.dispatchEvent(new Event("change", { bubbles: true }));

    console.log(`Attached "${TEST_FILE_NAME}" (${TEST_FILE_MIME_TYPE}, ${content.length} bytes).`);
}

(function () {
    const content = prompt(
        "Enter the text content for the test file:",
        "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*"
    );
    if (content === null) {
        console.log("Cancelled");
        return;
    }
    uploadTestCertificateFile(content);
})();
