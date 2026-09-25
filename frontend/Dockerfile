FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Flatten output regardless of Angular builder version (old vs new esbuild)
RUN if [ -d dist/frontend/browser ]; then mv dist/frontend/browser /app/output; \
    else mv dist/frontend /app/output; fi

FROM nginx:alpine
COPY --from=build /app/output /usr/share/nginx/html
EXPOSE 80
