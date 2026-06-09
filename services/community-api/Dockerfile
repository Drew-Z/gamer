FROM node:22-alpine

WORKDIR /app

COPY . .

RUN npm install --omit=dev --ignore-scripts --workspace @gamer/community-api

ENV PORT=4000
EXPOSE 4000

CMD ["node", "services/community-api/src/server.js"]
