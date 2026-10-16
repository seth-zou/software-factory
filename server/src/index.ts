import express, { type Request, type Response } from 'express'

const app = express()
const PORT = 3001

app.get('/ping', (_req: Request, res: Response) => {
  res.json({
    message: 'pong',
    timestamp: new Date().toISOString(),
  })
})

app.listen(PORT, () => {
  console.log(`Server is running on http://localhost:${PORT}`)
})
