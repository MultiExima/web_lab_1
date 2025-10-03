
(function () {
    function validateNumber(inputValue, min, max) {
        const trimmed = (inputValue || "").trim().replace(",", ".");
        if (trimmed.length === 0) {
            return { valid: false, value: null, error: "Пустое значение" };
        }

        const numberPattern = /^-?\d*(?:\.\d+)?$/;
        if (!numberPattern.test(trimmed)) {
            return { valid: false, value: null, error: "Только числа, точка как разделитель" };
        }

        if (trimmed === "-" || trimmed === "." || trimmed === "-.") {
            return { valid: false, value: null, error: "Неполное число" };
        }

        const numeric = Number(trimmed);
        if (Number.isNaN(numeric)) {
            return { valid: false, value: null, error: "Не число" };
        }

        if (numeric < min || numeric > max) {
            return { valid: false, value: null, error: `Диапазон от ${min} до ${max}` };
        }

        return { valid: true, value: numeric, error: null };
    }

    const form = document.getElementById("coords-form");
    const xInput = document.getElementById("x-input");
    const yInput = document.getElementById("y-input");
    const rButtons = Array.from(document.querySelectorAll(".r-button"));
    const resultsTbody = document.querySelector("table.res-table tbody");

    let selectedR = null;

    function updateRSelection(newR) {
        selectedR = newR;
        rButtons.forEach((btn) => {
            if (btn.dataset.value === String(newR)) {
                btn.classList.add("active");
                btn.setAttribute("aria-pressed", "true");
            } else {
                btn.classList.remove("active");
                btn.setAttribute("aria-pressed", "false");
            }
        });
    }

    rButtons.forEach((btn) => {
        btn.addEventListener("click", () => {
            const value = Number(btn.dataset.value);
            if (value >= 1 && value <= 5) {
                updateRSelection(value);
            }
        });
    });

    function sanitizeOnInput(el, allowMinus) {
        el.addEventListener("input", () => {
            let v = el.value.replace(",", ".");
            const allowed = allowMinus ? /[^0-9.-]/g : /[^0-9.]/g;
            v = v.replace(allowed, "");
            const parts = v.split(".");
            if (parts.length > 2) {
                v = parts.shift() + "." + parts.join("");
            }

            if (allowMinus) {
                const minusCount = (v.match(/-/g) || []).length;
                if (minusCount > 1) {
                    v = v.replace(/-/g, "");
                    v = "-" + v;
                }
                if (v.indexOf("-") > 0) {
                    v = v.replace(/-/g, "");
                    v = "-" + v;
                }
            } else {
                v = v.replace(/-/g, "");
            }
            el.value = v;
        });
    }

    sanitizeOnInput(xInput, true);
    sanitizeOnInput(yInput, true);

    function blockInvalidKeys(e, allowMinus) {
        const key = e.key;
        const isDigit = /\d/.test(key);
        const isControl = ["Backspace", "Delete", "ArrowLeft", "ArrowRight", "Tab", "Home", "End"].includes(key);
        const isDot = key === "." || key === ",";
        const isMinus = key === "-";

        if (isControl) return;

        if (isDigit) return;

        if (isDot) return;

        if (allowMinus && isMinus) {
            const el = e.target;
            if (el.selectionStart === 0 && !el.value.includes("-")) return;
        }

        e.preventDefault();
    }

    xInput.addEventListener("keydown", (e) => blockInvalidKeys(e, true));
    yInput.addEventListener("keydown", (e) => blockInvalidKeys(e, true));

    function formatNumber(num) {
        const str = String(num);
        const parts = str.split('.');
        
        if (parts.length === 1) {
            return str;
        }
        
        if (parts[1].length <= 3) {
            return str;
        }
        
        return parts[0] + '.' + parts[1].substring(0, 3) + '...';
    }

    function appendResultRow(result, scriptTime) {
        if (!resultsTbody) return;
        const tr = document.createElement("tr");
        const hitText = result.hit ? "Да" : "Нет";
        tr.innerHTML = `
            <td title="${result.x}">${formatNumber(result.x)}</td>
            <td title="${result.y}">${formatNumber(result.y)}</td>
            <td title="${result.r}">${formatNumber(result.r)}</td>
            <td>${hitText}</td>
            <td>${result.time || ""}</td>
            <td>${scriptTime || "N/A"}</td>
        `;
        resultsTbody.prepend(tr);
    }

    form.addEventListener("submit", (e) => {
        e.preventDefault();

        const xv = validateNumber(xInput.value, -5, 5);
        const yv = validateNumber(yInput.value, -3, 3);

        if (!selectedR) {
            alert("Партия требует оценку! Выберите R!");
            return;
        }

        if (!xv.valid) {
            alert(`Партия не одобряет такой X: ${xv.error}`);
            xInput.focus();
            return;
        }
        if (!yv.valid) {
            alert(`Партия не одобряет такой Y: ${yv.error}`);
            yInput.focus();
            return;
        }

        const params = new URLSearchParams({ x: String(xv.value), y: String(yv.value), r: String(selectedR) });
        
        const scriptStartTime = performance.now();
        
        fetch(`/api/hitcheck?${params.toString()}`, {
            method: "GET",
            headers: { "Accept": "application/json" }
        })
            .then(async (res) => {
                const data = await res.json().catch(() => null);
                if (!res.ok) {
                    const msg = data && data.error ? data.error : `HTTP ${res.status}`;
                    throw new Error(msg);
                }
                return data;
            })
            .then((data) => {
                const scriptEndTime = performance.now();
                const scriptExecutionTime = ((scriptEndTime - scriptStartTime) / 1000).toFixed(4) + " сек";
                appendResultRow(data, scriptExecutionTime);
            })
            .catch((err) => {
                alert(`Партия недовольна: ${err.message}`);
            });
    });
})();