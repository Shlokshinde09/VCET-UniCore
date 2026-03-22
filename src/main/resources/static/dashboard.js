const API_BASE_URL = 'http://localhost:8080';

// Global Chart instances
let trendChartInstance = null;
let adminChartInstance = null;

// Ensure we have a student ID
const studentId = localStorage.getItem('studentId') || 1; // Default to 1 for demo if missing

/** ---------------- STUDENT DASHBOARD ---------------- **/
async function initStudentDashboard() {
    try {
        // Fetch core KPIs concurrently where applicable
        const [cgpa, readiness, aiCgpa, aiTarget, trendData, advisor, companies, rawResults] = await Promise.all([
            fetchData(`/results/cgpa/${studentId}`),
            fetchData(`/analysis/readiness/${studentId}`),
            fetchData(`/analysis/ai-cgpa/${studentId}`),
            fetchData(`/analysis/target/${studentId}`),
            fetchData(`/analysis/trend/${studentId}`),
            fetchData(`/analysis/advisor/${studentId}`),
            fetchData(`/analysis/companies/${studentId}`),
            fetchData(`/results/student/${studentId}`) // Fetches List<Result>
        ]);

        // Process data for UI
        document.getElementById('cgpa').innerText = cgpa || '0.0';
        document.getElementById('readinessScore').innerText = readiness ? readiness.readinessScore + '%' : '0%';
        document.getElementById('aiCgpa').innerText = aiCgpa && aiCgpa.predictedFinalCGPA !== undefined ? Number(aiCgpa.predictedFinalCGPA).toFixed(2) : '--';
        document.getElementById('targetSgpa').innerText = aiTarget && aiTarget.aiTargetSGPA !== undefined ? Number(aiTarget.aiTargetSGPA).toFixed(2) : '--';

        // Readiness Color logic
        const readinessEl = document.getElementById('readinessScore');
        const readLvl = document.getElementById('readinessLevel');
        
        let readVal = readiness ? readiness.readinessScore : 0;
        if (readVal > 80) { readinessEl.style.color = 'var(--accent-green)'; readLvl.innerText = 'Highly Prepared'; }
        else if (readVal > 60) { readinessEl.style.color = 'var(--accent-orange)'; readLvl.innerText = 'Needs Improvement'; }
        else { readinessEl.style.color = 'var(--accent-red)'; readLvl.innerText = 'Critical Action Needed'; }

        // Advisor & Companies
        document.getElementById('adviceText').innerText = advisor?.advice || "Keep working hard!";
        renderTags('subjectTags', advisor?.weakSubjects, 'badge-weak');
        
        let placements = typeof companies === 'string' ? "Not easily available." : "Eligible for multiple roles.";
        document.getElementById('placementDesc').innerText = placements;
        if(Array.isArray(companies)) {
            renderTags('companiesList', companies, 'badge-strong');
        }

        // Subject Performance Table & Risk Detection
        let weakCount = 0;
        let isDeclining = false;
        
        if (Array.isArray(rawResults)) {
            const tableBody = document.getElementById('subjectTableBody');
            tableBody.innerHTML = '';

            // Filter out old legacy null results without semantics
            let validResults = rawResults.filter(r => r.courseCode != null && r.semester > 0);

            // Sort by semester descending
            validResults.sort((a,b) => b.semester - a.semester);

            validResults.forEach(res => {
                const gp = res.gradePoint || 0;
                let status = 'Average';
                let sClass = 'badge-average';
                
                if (gp > 8) { status = 'Strong'; sClass = 'badge-strong'; }
                else if (gp < 6) { status = 'Weak'; sClass = 'badge-weak'; weakCount++; }

                tableBody.innerHTML += `
                    <tr>
                        <td><strong>${res.courseName || res.courseCode || 'N/A'}</strong></td>
                        <td>Sem ${res.semester || '-'}</td>
                        <td>${res.credits || '-'}</td>
                        <td style="font-weight:600;">${gp}</td>
                        <td><span class="badge ${sClass}">${status}</span></td>
                    </tr>
                `;
            });
        }

        // Render Chart
        if (trendData && trendData.semesterSgpa && typeof trendData.semesterSgpa === 'object') {
            const semMap = trendData.semesterSgpa;
            const labels = Object.keys(semMap).map(sem => `Sem ${sem}`);
            const data = Object.values(semMap);
            
            // Use trend string from backend (Improving / Declining / Stable)
            const apiTrend = trendData.trend || 'Stable';
            isDeclining = apiTrend === 'Declining';

            // Set trend label
            const trendLabel = isDeclining ? 'Declining Trend' : (apiTrend === 'Improving' ? 'Upward Trend ↑' : 'Stable Trend');
            document.getElementById('trend').innerText = trendLabel;
            document.getElementById('trend').style.color = isDeclining ? 'var(--accent-red)' : 'var(--accent-green)';

            renderStudentChart(labels, data);
        }

        // Risk detection: only show for genuinely bad performance (not just slight decline)
        if (cgpa < 6.5 || weakCount >= 2) {
            document.getElementById('riskBanner').classList.remove('hidden');
            let riskMsg = "Academic risk detected: ";
            if(cgpa < 6.5) riskMsg += "CGPA is critically low. ";
            if(weakCount > 0) riskMsg += `You have ${weakCount} weak subject(s). Focus on improving them.`;
            document.getElementById('riskMessage').innerText = riskMsg;
        } else {
            document.getElementById('riskBanner').classList.add('hidden');
        }

        // Reveal dashboard smoothly
        document.getElementById('dashboardContent').style.opacity = 1;

    } catch (error) {
        console.error("Dashboard Init Error:", error);
        alert("Could not load some dashboard data. Is the backend running?");
        document.getElementById('dashboardContent').style.opacity = 1;
    }
}

/** ---------------- ADMIN DASHBOARD ---------------- **/
async function initAdminDashboard() {
    try {
        // Fetch admin data (Assuming /students returns list of students)
        const students = await fetchData('/students');
        
        if (Array.isArray(students)) {
            document.getElementById('adminTotalStudents').innerText = students.length;
            
            // Populate student roster table
            const tbody = document.getElementById('adminStudentsTableBody');
            tbody.innerHTML = '';
            students.forEach(s => {
                tbody.innerHTML += `
                    <tr>
                        <td>${s.id}</td>
                        <td><strong>${s.name}</strong></td>
                        <td>${s.email}</td>
                        <td>${s.department}</td>
                        <td>Sem ${s.semester}</td>
                        <td>
                            <button class="btn-outline" style="padding:4px 8px; font-size:12px;">Edit</button>
                        </td>
                    </tr>
                `;
            });

            // Mocking aggregated data since no explicit analytics API exists yet on backend
            document.getElementById('adminAvgCgpa').innerText = '7.45';
            document.getElementById('adminPlacementReady').innerText = Math.floor(students.length * 0.45);
            document.getElementById('adminRiskCount').innerText = Math.floor(students.length * 0.15);
            
            renderAdminChart();
            
            // Wiring up forms for future backend implementation
            hookupAdminForms();
        }
    } catch(err) {
        console.error("Admin Load Error:", err);
    }
}

function switchTab(tabId) {
    // Hide all tabs
    document.querySelectorAll('.tab-pane').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.tab-pane').forEach(el => el.classList.add('hidden'));
    
    // Remove active from nav links
    document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));

    // Show target tab
    const target = document.getElementById(`tab-${tabId}`);
    if(target) {
        target.classList.remove('hidden');
        target.classList.add('active');
        
        // Highlight nav item
        const navEvent = event ? event.target : document.querySelector('.nav-item');
        if(navEvent && navEvent.classList) navEvent.classList.add('active');
    }
}

function hookupAdminForms() {
    // These intercept forms and would conceptually do POST fetch() calls
    document.getElementById('addStudentForm')?.addEventListener('submit', (e) => {
        e.preventDefault(); alert('Student Add API connection pending.'); e.target.reset();
    });
    document.getElementById('addSubjectForm')?.addEventListener('submit', (e) => {
        e.preventDefault(); alert('Subject Add API connection pending.'); e.target.reset();
    });
    document.getElementById('addResultForm')?.addEventListener('submit', (e) => {
        e.preventDefault(); alert('Result Upload API connection pending.'); e.target.reset();
    });
}

/** ---------------- UTILITIES & CHARTS ---------------- **/

async function fetchData(endpoint) {
    try {
        const response = await fetch(API_BASE_URL + endpoint);
        if (!response.ok) throw new Error('API Error: ' + response.status);
        const ct = response.headers.get("content-type");
        if (ct && ct.includes("application/json")) {
            return await response.json();
        }
        return await response.text();
    } catch (error) {
        console.warn(`Fetch failed for ${endpoint}:`, error);
        return null;
    }
}

function renderTags(elementId, items, cssClass) {
    const el = document.getElementById(elementId);
    if(!el || !items) return;
    el.innerHTML = items.map(item => `<span class="badge ${cssClass}">${item}</span>`).join(' ');
}

function logout() {
    localStorage.removeItem('studentId');
    window.location.href = 'index.html';
}

function renderStudentChart(labels, data) {
    const ctx = document.getElementById('trendChart');
    if (!ctx) return;
    if (trendChartInstance) trendChartInstance.destroy();

    const gradient = ctx.getContext('2d').createLinearGradient(0, 0, 0, 300);
    gradient.addColorStop(0, 'rgba(59, 130, 246, 0.4)');
    gradient.addColorStop(1, 'rgba(59, 130, 246, 0.05)');

    trendChartInstance = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: 'SGPA',
                data: data,
                borderColor: '#3b82f6',
                backgroundColor: gradient,
                borderWidth: 3,
                fill: true,
                tension: 0.4,
                pointBackgroundColor: '#ffffff',
                pointBorderColor: '#3b82f6',
                pointRadius: 4,
                pointHoverRadius: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                y: { min: 0, max: 10, grid: { color: 'rgba(255,255,255,0.05)' } },
                x: { grid: { display: false } }
            }
        }
    });
}

function renderAdminChart() {
    const ctx = document.getElementById('adminDeptChart');
    if (!ctx) return;
    if(adminChartInstance) adminChartInstance.destroy();
    
    adminChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['Computer', 'IT', 'EXTC', 'Mechanical'],
            datasets: [{
                label: 'Average CGPA',
                data: [7.8, 7.5, 7.1, 6.9],
                backgroundColor: ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6'],
                borderRadius: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                y: { min: 0, max: 10, grid: { color: 'rgba(255,255,255,0.05)' } },
                x: { grid: { display: false } }
            }
        }
    });
}
