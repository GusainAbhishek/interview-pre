FROM node:18-alpine
WORKDIR /app
#.env won't be copied (in .gitignore
COPY . .        
RUN npm install
CMD ["node", "app.js"]
