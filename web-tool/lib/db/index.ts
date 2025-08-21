import { drizzle } from 'drizzle-orm/postgres-js'
import postgres from 'postgres'
import * as schema from './schema'

// Get the database URL from environment variable
const connectionString = process.env.DATABASE_URL!

if (!connectionString) {
  throw new Error('DATABASE_URL environment variable is not set')
}

// Create the postgres client
// Note: prepare: false is required for Supabase
const client = postgres(connectionString, {
  prepare: false,
  ssl: process.env.NODE_ENV === 'production' ? 'require' : false
})

// Create the drizzle instance with schema
export const db = drizzle(client, { schema })

// Export all schema for easy access
export * from './schema'