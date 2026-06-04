FROM node:22-alpine

WORKDIR /app

COPY . .

ENV PORT=4200
ENV COMMUNITY_API_URL=http://community-api:4000
EXPOSE 4200

CMD ["node", "apps/admin-review/server.js"]
