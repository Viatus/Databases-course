import dotenv from 'dotenv';

dotenv.config();

export default {
  database_url: "postgres://"+process.env.DATABASE_USERNAME+":"+process.env.DATABASE_PASSWORD+"@"+process.env.DATABASE_HOST+":"+process.env.DATABASE_PORT+"/"+process.env.DATABASE_NAME,
  secret: process.env.SECRET,
  port: process.env.PORT,
}