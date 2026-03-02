FROM golang:1.22-alpine AS builder

WORKDIR /app

COPY go.mod go.sum ./
RUN go mod download

COPY main.go .
RUN CGO_ENABLED=0 GOOS=linux go build -o golang-prometheus-demo .

# ── Final minimal image ───────────────────────────────────────────────────────
FROM scratch

COPY --from=builder /app/golang-prometheus-demo /golang-prometheus-demo

EXPOSE 8000

ENTRYPOINT ["/golang-prometheus-demo"]
