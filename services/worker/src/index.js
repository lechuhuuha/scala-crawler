const express = require("express");
const axios = require("axios");
const pino = require("pino");

const logger = pino({ level: process.env.LOG_LEVEL || "info" });
const app = express();

const port = Number(process.env.WORKER_HTTP_PORT || 3000);
const controlPlaneBaseUrl = process.env.CONTROL_PLANE_BASE_URL || "http://localhost:8080";
const pollIntervalMs = Number(process.env.WORKER_POLL_INTERVAL_MS || 5000);

app.get("/health", (_req, res) => {
  res.status(200).json({ service: "worker", status: "ok" });
});

app.listen(port, () => {
  logger.info({ port, controlPlaneBaseUrl }, "worker started");
});

setInterval(async () => {
  try {
    const response = await axios.get(`${controlPlaneBaseUrl}/health`, { timeout: 2000 });
    logger.debug({ status: response.status }, "control-plane heartbeat ok");
  } catch (error) {
    logger.warn({ err: error.message }, "control-plane heartbeat failed");
  }
}, pollIntervalMs);
