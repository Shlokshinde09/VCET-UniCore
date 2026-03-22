document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('loginForm');
    const errorMessage = document.getElementById('errorMessage');
    
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const role = document.getElementById('role').value;
        const email = document.getElementById('email').value.trim();
        const password = document.getElementById('password').value.trim();
        
        // Basic validation
        if (!email || !password) {
            showError("Please enter both email and password.");
            return;
        }

        const submitBtn = document.querySelector('.btn-primary');
        const originalText = submitBtn.innerText;
        submitBtn.innerText = "Authenticating...";
        submitBtn.disabled = true;
        
        try {
            if(role === 'student') {
                // Students authenticate via existing auth controller
                const response = await fetch(`http://localhost:8080/auth/login?email=${encodeURIComponent(email)}`, {
                    method: 'POST'
                });
                
                if(!response.ok) {
                    throw new Error('Authentication failed');
                }
                
                const studentData = await response.json();
                
                if(studentData && studentData.id) {
                    // Success
                    localStorage.setItem('studentId', studentData.id);
                    // Add a tiny delay for visual smoothness
                    setTimeout(() => {
                        window.location.href = "student-dashboard.html";
                    }, 400);
                } else {
                    showError("Invalid credentials or student not found.");
                }
            } else if (role === 'admin') {
                // Basic admin structure validation
                // Currently bypassing real DB check since admin structure isn't built yet,
                // but demonstrating the flow.
                if(email === 'admin@vcet.edu.in' && password === 'admin') {
                    setTimeout(() => {
                        // Redirect to admin dashboard
                        window.location.href = "admin-dashboard.html";
                    }, 500);
                } else {
                    showError("Invalid administrator credentials.");
                }
            }
            
        } catch (err) {
            console.error(err);
            showError("An error occurred connecting to the server.");
        } finally {
            if(role === 'student') {
                submitBtn.innerText = originalText;
                submitBtn.disabled = false;
            }
        }
    });
});

function showError(msg) {
    const errorEl = document.getElementById('errorMessage');
    errorEl.innerText = msg;
    errorEl.style.opacity = 0;
    
    // Quick fade in
    setTimeout(() => {
        errorEl.style.transition = 'opacity 0.3s ease';
        errorEl.style.opacity = 1;
    }, 10);
}

function forgotPassword() {
    alert("Password reset instructions have been sent to your registered email or contact the college administration.");
}
