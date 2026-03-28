import axios from 'axios';

/** EventSource ve tam URL kurulumu için (axios ile aynı kök) */
export const API_BASE = 'http://localhost:8080/api';

export const apiClient = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
});

