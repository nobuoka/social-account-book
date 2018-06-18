FROM node:9-alpine

COPY . /test-app
WORKDIR /test-app
RUN npm install
RUN npm run-script build

CMD ["node", "index.js"]
