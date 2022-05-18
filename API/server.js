import express from 'express';
import 'babel-polyfill';
import cors from 'cors';
import env from './env';
import usersRoute from './app/routes/usersRoute';
import gamesRoute from './app/routes/gamesRoute';
import machineUsageRoute from './app/routes/machineUsageRoute';
import subscriptionRoute from './app/routes/subscriptionRoute';

const app = express();

app.use(cors());

app.use(express.urlencoded({ extended: false }));
app.use(express.json());

app.use('/', usersRoute);
app.use('/', gamesRoute);
app.use('/', machineUsageRoute);
app.use('/', subscriptionRoute);

app.listen(env.port).on('listening', () => {
  console.log(`are live on ${env.port}`);
});

export default app;