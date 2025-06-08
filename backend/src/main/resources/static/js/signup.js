let pollingInterval;
let lastPolledEmail = null;
let verified = false;
let pollingStarted = false;

const emailInput = document.getElementById("email-input");
const sendEmailBtn = document.getElementById("send-email-btn");
const changeEmailBtn = document.getElementById("change-email-btn");
const status = document.getElementById("email-verification-status");
const submitBtn = document.getElementById("submit-btn");

const passwordInput = document.getElementById("password-input");
const confirmPasswordInput = document.getElementById("confirm-password-input");
const matchMessage = document.getElementById("password-match-message");

passwordInput.addEventListener("focus", () => {
    passwordInput.classList.remove("error-input");
});

sendEmailBtn.addEventListener("click", () => {
    const email = emailInput.value.trim();
    alert("Sent verification email.");

    lastPolledEmail = email;
    verified = false;
    emailInput.readOnly = false;
    emailInput.classList.remove("readonly-input");
    submitBtn.disabled = true;
    startPolling(email);

    fetch('/api/v1/members/email-verification/request', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: email })
    }).then(res => {
        if (!res.ok) {
            alert("메일 전송에 실패했습니다.");
        }
    });
});

emailInput.addEventListener("input", () => {
    const current = emailInput.value;

    if (current !== lastPolledEmail) {
        verified = false;
        emailInput.readOnly = false;
        emailInput.classList.remove("readonly-input");
        submitBtn.disabled = true;
        status.textContent = "❌ Needs Verification";
        status.style.color = "red";
        if (pollingStarted) {
            clearInterval(pollingInterval);
            pollingStarted = false;
        }
    }
});

if (changeEmailBtn) {
    changeEmailBtn.addEventListener("click", () => {
        emailInput.readOnly = false;
        emailInput.classList.remove("readonly-input");
        verified = false;
        submitBtn.disabled = true;
        status.textContent = "❌ Needs Verification";
        status.style.color = "red";
        changeEmailBtn.style.display = "none";
    });
}

confirmPasswordInput.addEventListener("input", () => {
    if (confirmPasswordInput.value === passwordInput.value) {
        matchMessage.textContent = "✅ 패스워드 일치";
        matchMessage.style.color = "green";
    } else {
        matchMessage.textContent = "❌ 패스워드 불일치";
        matchMessage.style.color = "red";
    }

    checkFormValidity();
});

document.addEventListener("DOMContentLoaded", () => {
    if (initialVerified) {
        emailInput.readOnly = true;
        emailInput.classList.add("readonly-input");
        submitBtn.disabled = false;
        status.textContent = "✅ Verified";
        status.style.color = "green";
        changeEmailBtn.style.display = "inline-block";
        verified = true;
    }

    if (globalError) {
        document.getElementById("global-error-message").textContent = "회원가입 실패: " + globalError;
        document.getElementById("global-error-modal").style.display = "flex";
    }
});

function closeModal() {
    document.getElementById("global-error-modal").style.display = "none";
}

function startPolling(email) {
    pollingStarted = true;

    pollingInterval = setInterval(() => {
        fetch(`/api/v1/members/email-verification/status?email=${encodeURIComponent(email)}`)
            .then(res => res.json())
            .then(data => {
                const isVerified = data.data;
                if (isVerified) {
                    clearInterval(pollingInterval);
                    verified = true;

                    // 인증 완료 UI 처리
                    emailInput.readOnly = true;
                    emailInput.classList.add("readonly-input");
                    status.textContent = "✅ Verified";
                    status.style.color = "green";
                    checkFormValidity(); // submit 버튼 활성화 가능 여부 확인

                    // ✅ 이메일 변경 버튼 표시
                    changeEmailBtn.style.display = "inline-block";
                }
            });
    }, 1000);
}

function checkFormValidity() {
    const passwordsMatch = confirmPasswordInput.value === passwordInput.value;
    const isFormValid = verified && passwordsMatch;

    submitBtn.disabled = !isFormValid;
}
