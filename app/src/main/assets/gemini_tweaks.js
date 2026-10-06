// ========================================================
// GeminiDuck Custom JS: UX Tweaks, Auto-focus & Markdown Export
// ========================================================

(function () {
    if (window.__geminiDuckLoaded) return;
    window.__geminiDuckLoaded = true;

    console.log("[GeminiDuck] Initializing tweaks...");

    // 1. Auto-focus & Wklejanie udostępnionego tekstu
    function tryAutoFocus() {
        const inputField = document.querySelector('.ql-editor, rich-textarea div[contenteditable="true"], div[contenteditable="true"], textarea');
        if (inputField) {
            inputField.focus();
        }
    }

    window.insertSharedText = function(text, retries = 10) {
        if (!text) return;
        const editor = document.querySelector('.ql-editor, rich-textarea div[contenteditable="true"], div[contenteditable="true"], textarea');
        if (editor) {
            editor.focus();
            let success = false;
            try {
                // Najbardziej niezawodny sposób dla edytorów typu Quill/Angular
                success = document.execCommand('insertText', false, text);
            } catch (e) {
                console.error("execCommand failed", e);
            }

            if (!success) {
                if (editor.tagName === 'TEXTAREA' || editor.tagName === 'INPUT') {
                    editor.value = (editor.value ? editor.value + "\n" : "") + text;
                } else {
                    editor.innerText = (editor.innerText ? editor.innerText + "\n" : "") + text;
                }
                editor.dispatchEvent(new Event('input', { bubbles: true }));
                editor.dispatchEvent(new Event('change', { bubbles: true }));
            }
            showToast("Wklejono udostępniony tekst do Gemini!");
        } else if (retries > 0) {
            setTimeout(() => window.insertSharedText(text, retries - 1), 600);
        }
    };

    // 2. Export chat to Markdown format
    window.exportChatToMarkdown = function () {
        const queries = document.querySelectorAll('.user-query-container, [data-test-id="user-query"]');
        const responses = document.querySelectorAll('.model-response-text, model-response, [data-test-id="model-response"]');

        let markdown = `# Eksport rozmowy Gemini (${new Date().toLocaleString()})\n\n`;

        const turns = Math.max(queries.length, responses.length);
        if (turns === 0) {
            alert("Nie znaleziono jeszcze żadnych wiadomości w tym czacie.");
            return;
        }

        for (let i = 0; i < turns; i++) {
            if (queries[i]) {
                const userText = queries[i].innerText.trim();
                markdown += `### 👤 Ty:\n${userText}\n\n`;
            }
            if (responses[i]) {
                const modelText = responses[i].innerText.trim();
                markdown += `### 🤖 Gemini:\n${modelText}\n\n---\n\n`;
            }
        }

        // Kopiowanie do schowka lub pobranie jako plik .md
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(markdown).then(() => {
                showToast("Skopiowano całą rozmowę do schowka w formacie Markdown!");
            }).catch(() => {
                downloadFile(markdown, "gemini-chat.md");
            });
        } else {
            downloadFile(markdown, "gemini-chat.md");
        }
    };

    function downloadFile(content, filename) {
        const blob = new Blob([content], { type: "text/markdown;charset=utf-8" });
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }

    function showToast(message) {
        let toast = document.getElementById("geminiduck-toast");
        if (!toast) {
            toast = document.createElement("div");
            toast.id = "geminiduck-toast";
            toast.style.cssText = `
                position: fixed;
                bottom: 24px;
                left: 50%;
                transform: translateX(-50%);
                background: #1a73e8;
                color: #ffffff;
                padding: 10px 20px;
                border-radius: 20px;
                font-size: 14px;
                font-family: sans-serif;
                z-index: 999999;
                box-shadow: 0 4px 12px rgba(0,0,0,0.5);
                transition: opacity 0.3s;
            `;
            document.body.appendChild(toast);
        }
        toast.innerText = message;
        toast.style.opacity = "1";
        setTimeout(() => {
            toast.style.opacity = "0";
        }, 3000);
    }

    // 3. Dodanie pływającego przycisku narzędziowego (FAB)
    function setupUtilityButton() {
        if (document.getElementById("geminiduck-fab-container")) return;

        const container = document.createElement("div");
        container.id = "geminiduck-fab-container";

        const exportBtn = document.createElement("button");
        exportBtn.className = "geminiduck-fab-btn";
        exportBtn.title = "Eksportuj do Markdown";
        exportBtn.innerHTML = "📝";
        exportBtn.onclick = window.exportChatToMarkdown;

        container.appendChild(exportBtn);
        document.body.appendChild(container);
    }

    // 4. Inicjalizacja z opóźnieniem dla Single-Page Application (Angular)
    window.addEventListener("DOMContentLoaded", () => {
        setTimeout(tryAutoFocus, 1200);
        setTimeout(setupUtilityButton, 1500);
    });

    // Cykliczne sprawdzanie obecności FAB (na wypadek przeładowania widoku SPA)
    setInterval(() => {
        if (!document.getElementById("geminiduck-fab-container") && window.location.hostname.includes("gemini.google.com")) {
            setupUtilityButton();
        }
    }, 3000);

})();
