# Client

React + Vite frontend for the distributed code judge.

## Features

- Login/register flow (JWT bearer token stored in localStorage).
- Problem list popover drawer and Markdown + KaTeX rendering for statements.
- Code editor, submission, and polling results UI.

## Run locally

```bash
cd client
npm install
npm run dev
```

The UI expects the backend at `http://localhost:8080/api` by default. Override with `VITE_API_URL` if needed.
