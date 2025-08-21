#!/usr/bin/env node

const postgres = require('postgres');
require('dotenv').config({ path: '.env.local' });

const DATABASE_URL = process.env.DATABASE_URL;

if (!DATABASE_URL) {
  console.error('❌ DATABASE_URL is not set in .env.local');
  process.exit(1);
}

async function resetDatabase() {
  const sql = postgres(DATABASE_URL, {
    ssl: 'require'
  });

  try {
    console.log('🔄 Resetting database...\n');

    // Drop all tables
    console.log('📝 Dropping tables...');
    const tables = [
      'feedback',
      'submissions', 
      'parser_patterns',
      'message_templates',
      'daily_analytics',
      'categories',
      'banks'
    ];

    for (const table of tables) {
      try {
        await sql`DROP TABLE IF EXISTS ${sql.unsafe(table)} CASCADE`;
        console.log(`✅ Dropped table: ${table}`);
      } catch (error) {
        console.log(`⚠️  Could not drop ${table}: ${error.message}`);
      }
    }

    // Drop all types
    console.log('\n📝 Dropping types...');
    const types = [
      'message_category',
      'submission_status',
      'support_level',
      'transaction_type'
    ];

    for (const type of types) {
      try {
        await sql`DROP TYPE IF EXISTS ${sql.unsafe(type)} CASCADE`;
        console.log(`✅ Dropped type: ${type}`);
      } catch (error) {
        console.log(`⚠️  Could not drop ${type}: ${error.message}`);
      }
    }

    console.log('\n✅ Database reset complete!');
    console.log('Run "npm run db:migrate" to apply fresh migrations.');
  } catch (error) {
    console.error('❌ Reset failed:', error);
    process.exit(1);
  } finally {
    await sql.end();
  }
}

resetDatabase();