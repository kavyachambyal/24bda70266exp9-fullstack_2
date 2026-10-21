const activityList = document.querySelector("#activity-list");
const toast = document.querySelector("#toast");
let activityEvents = [];
let toastTimeout;

function showToast(message, isError = false) {
  toast.textContent = message;
  toast.classList.toggle("error", isError);
  toast.classList.add("visible");
  window.clearTimeout(toastTimeout);
  toastTimeout = window.setTimeout(() => toast.classList.remove("visible"), 3000);
}

async function api(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: { "Content-Type": "application/json", ...options.headers },
  });
  const data = await response.json();
  if (!response.ok) throw new Error(data.error || `Request failed (${response.status})`);
  return data;
}

function addActivity(title, detail) {
  activityEvents.unshift({ title, detail, time: new Intl.DateTimeFormat(undefined, { hour: "numeric", minute: "2-digit" }).format(new Date()) });
  activityEvents = activityEvents.slice(0, 4);
  renderActivity();
}

function renderActivity() {
  document.querySelector("#activity-count").textContent = `${activityEvents.length} ${activityEvents.length === 1 ? "event" : "events"}`;
  if (!activityEvents.length) {
    activityList.innerHTML = '<div class="empty-activity"><span class="empty-icon">✳</span><strong>It’s quiet in here</strong><span>Run your first pipeline to see activity.</span></div>';
    return;
  }
  activityList.innerHTML = activityEvents.map(({ title, detail, time }) => `
    <div class="activity-item"><span class="activity-mark">✓</span>
      <div class="activity-copy"><strong>${title}</strong><span>${detail}</span></div>
      <span class="activity-time">${time}</span>
    </div>`).join("");
}

function updateDashboard(data) {
  const { pipeline, deployment } = data;
  const hasRun = pipeline.runNumber > 0;
  document.querySelector("#pipeline-status").textContent = hasRun ? "All checks passed" : "Ready to run";
  document.querySelector("#pipeline-meta").innerHTML = hasRun
    ? '<span class="status-dot"></span> Latest run completed successfully'
    : '<span class="status-dot status-gray"></span> Waiting for your first run';
  document.querySelector("#build-number").textContent = hasRun ? `# ${String(pipeline.runNumber).padStart(4, "0")}` : "# —";
  document.querySelector("#build-meta").textContent = pipeline.lastRunAt ? `Completed ${new Date(pipeline.lastRunAt).toLocaleString()}` : "No builds yet";
  const runBadge = document.querySelector("#run-badge");
  runBadge.textContent = hasRun ? `RUN #${String(pipeline.runNumber).padStart(4, "0")} · PASSED` : "Awaiting first run";
  runBadge.classList.toggle("complete", hasRun);
  document.querySelector("#traffic-value").innerHTML = `${deployment.trafficPercent}<span class="stat-unit">%</span>`;
  document.querySelector("#traffic-label").textContent = `${deployment.trafficPercent}%`;
  document.querySelector("#traffic-progress").style.width = `${deployment.trafficPercent}%`;
  document.querySelector("#traffic-meta").innerHTML = deployment.trafficPercent
    ? '<span class="status-dot"></span> Canary receiving live traffic'
    : '<span class="status-dot status-gray"></span> No active deployment';
  const deploymentStatus = document.querySelector("#deployment-status");
  const labels = { idle: "Not started", canary: "Canary in progress", live: "Fully deployed" };
  deploymentStatus.innerHTML = `<i class="status-dot ${deployment.status === "idle" ? "status-gray" : ""}"></i> ${labels[deployment.status]}`;
  document.querySelector("#start-canary").disabled = deployment.status !== "idle" || !hasRun;
  document.querySelector("#promote-canary").disabled = deployment.status === "idle" || deployment.status === "live";
  document.querySelector("#promote-canary").textContent = deployment.status === "live" ? "Fully deployed" : "Promote release";
  document.querySelector("#rollback-canary").disabled = deployment.status === "idle";
  document.querySelector("#deploy-note").innerHTML = deployment.status === "idle"
    ? hasRun
      ? "<span>ⓘ</span> Start at 10%, monitor health, then promote in stages."
      : "<span>ⓘ</span> Run the pipeline successfully before starting a canary."
    : deployment.status === "live"
      ? '<span>✓</span> All traffic is on the new version. Release complete.'
      : `<span>ⓘ</span> Canary is serving ${deployment.trafficPercent}% of live traffic. Check health before promoting.`;
}

async function performAction(path, action) {
  const button = document.querySelector(`#${action}`);
  button.disabled = true;
  try {
    const data = await api(path, { method: "POST" });
    updateDashboard(data);
    return data;
  } catch (error) {
    showToast(error.message, true);
    await refreshOverview();
    return null;
  }
}

async function refreshOverview() {
  try {
    updateDashboard(await api("/api/overview"));
  } catch {
    document.querySelector("#pipeline-status").textContent = "API unavailable";
    document.querySelector("#pipeline-meta").innerHTML = '<span class="status-dot status-gray"></span> Start the Spring Boot server to connect';
    showToast("Could not connect to the API. Start the app with ./mvnw spring-boot:run.", true);
  }
}

document.querySelector("#run-pipeline").addEventListener("click", async (event) => {
  const button = event.currentTarget;
  button.disabled = true;
  button.innerHTML = '<span class="play-icon">◌</span> Running checks…';
  const data = await performAction("/api/pipeline/run", "run-pipeline");
  button.disabled = false;
  button.innerHTML = '<span class="play-icon">▶</span> Run pipeline';
  if (data) {
    addActivity("Pipeline passed", `Build #${String(data.pipeline.runNumber).padStart(4, "0")} · 8 tests green`);
    showToast("All pipeline checks passed.");
  }
});

document.querySelector("#start-canary").addEventListener("click", async () => {
  const data = await performAction("/api/deployment/canary", "start-canary");
  if (data) {
    addActivity("Canary deployment started", "Version v1.0.0 is receiving 10% of traffic");
    showToast("Canary started at 10% traffic.");
  }
});

document.querySelector("#promote-canary").addEventListener("click", async () => {
  const data = await performAction("/api/deployment/promote", "promote-canary");
  if (data) {
    addActivity(data.deployment.status === "live" ? "Release fully deployed" : "Canary promoted", `${data.deployment.trafficPercent}% of traffic shifted to v1.0.0`);
    showToast(data.deployment.status === "live" ? "Release is live to all users." : `Canary promoted to ${data.deployment.trafficPercent}%.`);
  }
});

document.querySelector("#rollback-canary").addEventListener("click", async () => {
  const data = await performAction("/api/deployment/rollback", "rollback-canary");
  if (data) {
    addActivity("Canary rolled back", "Traffic returned to the previous stable release");
    showToast("Rollback complete. Traffic is back on the stable version.");
  }
});

document.querySelectorAll(".nav-link").forEach((link) => {
  link.addEventListener("click", () => {
    document.querySelectorAll(".nav-link").forEach((item) => item.classList.remove("active"));
    link.classList.add("active");
  });
});

refreshOverview();
