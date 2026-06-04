FROM node:22-alpine

WORKDIR /app

COPY . .

ENV PORT=4100
EXPOSE 4100

CMD ["node", "services/pet-generator/src/server.js"]
