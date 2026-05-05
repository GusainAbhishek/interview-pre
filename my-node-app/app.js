const express = require('express');
const app = express();

const DB_HOST = process.env.DB_HOST;
const API_KEY = process.env.API_KEY;

if (!DB_HOST || !API_KEY) {
  console.error('FATAL: Missing required env variables');
  process.exit(1);   // ← THIS causes CrashLoopBackOff
}

app.listen(3000, () => console.log('Running on port 3000'));
 
