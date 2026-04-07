const mysql = require('mysql2/promise');
const fs = require('fs');

async function initDb() {
  try {
    const connection = await mysql.createConnection({
      host: 'localhost',
      user: 'root',
      password: 'tm050711',
      multipleStatements: true
    });
    console.log('Connected to MySQL. Creating Database...');
    await connection.query('CREATE DATABASE IF NOT EXISTS online_edu CHARACTER SET utf8mb4');
    await connection.query('USE online_edu');
    
    console.log('Reading schema.sql...');
    const sql = fs.readFileSync('schema.sql', 'utf8');
    
    console.log('Executing schema.sql...');
    await connection.query(sql);
    
    console.log('Database initialized successfully.');
    await connection.end();
  } catch (err) {
    console.error('Error in DB init:', err);
  }
}

initDb();
