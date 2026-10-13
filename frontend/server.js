const express = require('express');

const app = express();
const port = Number(process.env.PORT) || 5500;

app.use(express.static(__dirname));

app.listen(port, () => {
  console.log(`Employee Management frontend running at http://localhost:${port}`);
});
