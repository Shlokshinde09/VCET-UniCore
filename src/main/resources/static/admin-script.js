const API_BASE_URL = 'http://localhost:8080';

// Global Data
let studentsList = [];
let adminChartInstance = null;

async function initAdminDashboard() {
    await loadStudents();
    await loadSubjects();
    await loadResults();
    hookupAdminForms();
}

/** ---------------- TAB NAVIGATION ---------------- **/
function switchTab(tabId) {
    document.querySelectorAll('.tab-pane').forEach(el => {
        el.classList.remove('active');
        el.classList.add('hidden');
    });
    document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));

    const target = document.getElementById(`tab-${tabId}`);
    if(target) {
        target.classList.remove('hidden');
        target.classList.add('active');
        if(event && event.target) {
            event.target.classList.add('active');
        }
    }
}

/** ---------------- STUDENT MANAGEMENT ---------------- **/

async function loadStudents() {
    try {
        const response = await fetch(`${API_BASE_URL}/students`);
        if(!response.ok) throw new Error("Failed to load students");
        
        studentsList = await response.json();
        
        // Update Table
        const tbody = document.getElementById('adminStudentsTableBody');
        tbody.innerHTML = '';
        
        studentsList.forEach(s => {
            tbody.innerHTML += `
                <tr>
                    <td>${s.id}</td>
                    <td><strong>${s.name}</strong></td>
                    <td>${s.email}</td>
                    <td>${s.department}</td>
                    <td>Sem ${s.semester}</td>
                    <td>
                        <button class="btn-outline" style="border-color: var(--accent-red); color: var(--accent-red);" onclick="deleteStudent(${s.id})">Delete</button>
                    </td>
                </tr>
            `;
        });

        // Update Dashboard Home KPIs
        document.getElementById('adminTotalStudents').innerText = studentsList.length;
        
        // Compute live average CGPA for all students
        let totalCgpa = 0, validCount = 0;
        for (const s of studentsList) {
            try {
                const cgpaRes = await fetch(`${API_BASE_URL}/results/cgpa/${s.id}`);
                if (cgpaRes.ok) {
                    const cgpaVal = await cgpaRes.json();
                    if (cgpaVal > 0) { totalCgpa += cgpaVal; validCount++; }
                }
            } catch(_) {}
        }
        const avgCgpa = validCount > 0 ? (totalCgpa / validCount).toFixed(2) : '--';
        document.getElementById('adminAvgCgpa').innerText = avgCgpa;
        
        const placementReady = validCount > 0 ? Math.round(validCount * ((totalCgpa / validCount) >= 7 ? 0.8 : 0.3)) : 0;
        document.getElementById('adminPlacementReady').innerText = placementReady;
        document.getElementById('adminRiskCount').innerText = Math.max(0, studentsList.length - placementReady);
        
        renderAdminChart();

    } catch(err) {
        console.error(err);
        alert("Error loading students list.");
    }
}

async function addStudent(e) {
    e.preventDefault();
    const btn = e.target.querySelector('button[type="submit"]');
    btn.disabled = true;

    const studentData = {
        name: document.getElementById('studName').value.trim(),
        email: document.getElementById('studEmail').value.trim(),
        department: document.getElementById('studDept').value.trim(),
        semester: parseInt(document.getElementById('studSem').value, 10)
    };

    try {
        const response = await fetch(`${API_BASE_URL}/students`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(studentData)
        });

        if(!response.ok) throw new Error("Failed to add student");

        alert("Student added successfully!");
        e.target.reset(); // Clear form
        await loadStudents(); // Refresh table
    } catch(err) {
        console.error(err);
        alert("Failed to add student. Ensure backend is running.");
    } finally {
        btn.disabled = false;
    }
}

async function deleteStudent(id) {
    if(!confirm(`Are you sure you want to delete Student ID: ${id}?`)) return;

    try {
        const response = await fetch(`${API_BASE_URL}/students/${id}`, {
            method: 'DELETE'
        });

        if(!response.ok) throw new Error("Failed to delete student");

        alert("Student deleted successfully!");
        await loadStudents(); // Refresh table
    } catch(err) {
        console.error(err);
        alert("Failed to delete student. They might have dependent results.");
    }
}

/** ---------------- SUBJECT MANAGEMENT ---------------- **/

async function addSubject(e) {
    e.preventDefault();
    const btn = e.target.querySelector('button[type="submit"]');
    btn.disabled = true;

    const subjectData = {
        subjectName: document.getElementById('subName').value.trim(),
        credits: parseInt(document.getElementById('subCredits').value, 10),
        semester: parseInt(document.getElementById('subSem').value, 10)
    };

    try {
        const response = await fetch(`${API_BASE_URL}/subjects`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(subjectData)
        });

        if(!response.ok) throw new Error("Failed to add subject");

        alert("Subject added successfully!");
        e.target.reset();
        await loadSubjects(); // Refresh
    } catch(err) {
        console.error(err);
        alert("Failed to add subject.");
    } finally {
        btn.disabled = false;
    }
}

async function loadSubjects() {
    try {
        const response = await fetch(`${API_BASE_URL}/subjects`);
        if(!response.ok) throw new Error("Failed to load subjects");
        const subjectsList = await response.json();
        
        const tbody = document.getElementById('adminSubjectsTableBody');
        tbody.innerHTML = '';
        
        subjectsList.forEach(sub => {
            tbody.innerHTML += `
                <tr>
                    <td>${sub.id}</td>
                    <td><strong>${sub.subjectName}</strong></td>
                    <td>${sub.credits}</td>
                    <td>Sem ${sub.semester}</td>
                    <td>
                        <button class="btn-outline" style="border-color: var(--accent-red); color: var(--accent-red); padding:4px 8px; font-size:12px;" onclick="deleteSubject(${sub.id})">Delete</button>
                    </td>
                </tr>
            `;
        });
    } catch(err) {
        console.error(err);
    }
}

async function deleteSubject(id) {
    if(!confirm(`Are you sure you want to delete Subject ID: ${id}?`)) return;

    try {
        const response = await fetch(`${API_BASE_URL}/subjects/${id}`, {
            method: 'DELETE'
        });

        if(!response.ok) throw new Error("Failed to delete subject");

        alert("Subject deleted successfully!");
        await loadSubjects(); // Refresh table
    } catch(err) {
        console.error(err);
        alert("Failed to delete subject. Results may be dependent on it.");
    }
}

/** ---------------- RESULT MANAGEMENT ---------------- **/

async function uploadResult(e) {
    e.preventDefault();
    const btn = e.target.querySelector('button[type="submit"]');
    btn.disabled = true;

    const studentId = document.getElementById('resStudentId').value;
    const subjectId = document.getElementById('resSubjectId').value;
    const gradePoint = document.getElementById('resGrade').value;

    try {
        // ResultController expects RequestParams for POST /results
        const params = new URLSearchParams({
            studentId,
            subjectId,
            gradePoint
        });

        const response = await fetch(`${API_BASE_URL}/results?${params.toString()}`, {
            method: 'POST'
        });

        if(!response.ok) throw new Error("Failed to upload result");

        alert("Grade uploaded successfully!");
        e.target.reset();
        await loadResults(); // Refresh
    } catch(err) {
        console.error(err);
        alert("Failed to upload result. Ensure the Student ID and Subject ID exist.");
    } finally {
        btn.disabled = false;
    }
}

async function loadResults() {
    try {
        const response = await fetch(`${API_BASE_URL}/results`);
        if(!response.ok) throw new Error("Failed to load results");
        const resultsList = await response.json();
        
        const tbody = document.getElementById('adminResultsTableBody');
        tbody.innerHTML = '';
        
        // Show most recent first (assume last in array is newest)
        const recentResults = resultsList.slice().reverse().slice(0, 50); // Show max 50 recent
        
        recentResults.forEach(res => {
            const studentId = res.student ? res.student.id : 'N/A';
            const courseName = res.courseName || (res.subject ? res.subject.subjectName : 'N/A');
            const total = res.totalMarks || '-';
            const gp = res.gradePoint || '0';
            const cg = res.creditGrade || '-';
            
            tbody.innerHTML += `
                <tr>
                    <td><strong>#${studentId}</strong></td>
                    <td>${courseName}</td>
                    <td>Sem ${res.semester}</td>
                    <td>${total}</td>
                    <td><span class="badge ${gp > 6 ? 'badge-strong' : 'badge-weak'}">${gp}</span></td>
                    <td>${cg}</td>
                </tr>
            `;
        });
    } catch(err) {
        console.error("No global results /results endpoint found or error", err);
    }
}

/** ---------------- UTILITIES ---------------- **/

function hookupAdminForms() {
    document.getElementById('addStudentForm').addEventListener('submit', addStudent);
    document.getElementById('addSubjectForm').addEventListener('submit', addSubject);
    document.getElementById('addResultForm').addEventListener('submit', uploadResult);
}

function logout() {
    localStorage.removeItem('studentId');
    window.location.href = 'index.html';
}

async function loadAnalytics() {
    const tbody = document.getElementById('analyticsTableBody');
    if (!tbody) return;
    tbody.innerHTML = '<tr><td colspan="5" style="text-align:center; padding:1rem;">Loading...</td></tr>';

    // Fetch all students
    const studRes = await fetch(`${API_BASE_URL}/students`);
    const students = await studRes.json();

    // Fetch CGPA for each student
    const studentData = [];
    for (const s of students) {
        try {
            const cgpaRes = await fetch(`${API_BASE_URL}/results/cgpa/${s.id}`);
            const cgpa = cgpaRes.ok ? await cgpaRes.json() : 0;
            studentData.push({ ...s, cgpa: parseFloat(cgpa) || 0 });
        } catch(_) {
            studentData.push({ ...s, cgpa: 0 });
        }
    }

    // Sort by CGPA descending
    studentData.sort((a, b) => b.cgpa - a.cgpa);

    // Populate table
    tbody.innerHTML = '';
    studentData.forEach((s, idx) => {
        let badge = '';
        if (s.cgpa >= 7.5) badge = '<span style="color:#10b981; font-weight:600;">✓ Eligible for Placement Drive</span>';
        else if (s.cgpa >= 6.5) badge = '<span style="color:#f59e0b; font-weight:600;">⚠ May be Eligible (Check Cutoff)</span>';
        else if (s.cgpa > 0) badge = '<span style="color:#ef4444; font-weight:600;">✗ Below Placement Cutoff</span>';
        else badge = '<span style="color:#94a3b8;">No Results Yet</span>';

        const rankEmoji = idx === 0 ? '🥇' : idx === 1 ? '🥈' : idx === 2 ? '🥉' : `#${idx+1}`;
        tbody.innerHTML += `
            <tr style="cursor:pointer;" onclick="window.open('student-dashboard.html','_blank')">
                <td><strong>${rankEmoji}</strong></td>
                <td><strong>${s.name}</strong></td>
                <td>${s.department}</td>
                <td><strong style="font-size:1.1rem; color:${s.cgpa >= 7 ? '#10b981' : '#ef4444'}">${s.cgpa > 0 ? s.cgpa.toFixed(2) : '--'}</strong></td>
                <td>${badge}</td>
            </tr>
        `;
    });

    // Draw chart
    const ctx = document.getElementById('adminDeptChart');
    if (!ctx) return;
    if (adminChartInstance) adminChartInstance.destroy();

    const names = studentData.map(s => s.name);
    const cgpas = studentData.map(s => s.cgpa);
    const colors = cgpas.map(c => c >= 8 ? '#10b981' : c >= 7 ? '#3b82f6' : c >= 6 ? '#f59e0b' : '#ef4444');

    adminChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: names,
            datasets: [{
                label: 'CGPA',
                data: cgpas,
                backgroundColor: colors,
                borderRadius: 8
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: ctx => `CGPA: ${ctx.raw.toFixed(2)}`
                    }
                }
            },
            scales: {
                y: { min: 0, max: 10, grid: { color: 'rgba(255,255,255,0.05)' }, title: { display: true, text: 'CGPA', color: '#94a3b8' } },
                x: { grid: { display: false } }
            }
        }
    });
}
