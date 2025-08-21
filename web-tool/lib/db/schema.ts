import {
  pgTable,
  uuid,
  text,
  varchar,
  boolean,
  timestamp,
  jsonb,
  pgEnum,
  index,
  uniqueIndex,
  integer,
  decimal,
  foreignKey
} from 'drizzle-orm/pg-core'

// Enums
export const submissionStatusEnum = pgEnum('submission_status', [
  'pending', 'reviewing', 'approved', 'rejected', 'duplicate', 'implemented', 'improvement'
])

export const transactionTypeEnum = pgEnum('transaction_type', [
  'INCOME', 'EXPENSE'
])

export const messageCategoryEnum = pgEnum('message_category', [
  'debit', 'credit', 'alert', 'otp', 'promotional'
])

export const supportLevelEnum = pgEnum('support_level', [
  'full', 'partial', 'experimental', 'none'
])

// Banks table with RLS enabled
export const banks = pgTable('banks', {
  id: uuid('id').defaultRandom().primaryKey(),
  code: varchar('code', { length: 20 }).unique().notNull(),
  name: varchar('name', { length: 100 }).notNull(),
  fullName: varchar('full_name', { length: 255 }),

  // Configuration
  patterns: jsonb('patterns').$type<Record<string, any>>().default({}).notNull(),
  supportedFormats: text('supported_formats').array(),
  isActive: boolean('is_active').default(true).notNull(),
  supportLevel: supportLevelEnum('support_level').default('none').notNull(),

  // Stats
  totalSubmissions: integer('total_submissions').default(0).notNull(),
  successfulParses: integer('successful_parses').default(0).notNull(),
  successRate: decimal('success_rate', { precision: 5, scale: 2 }),

  // Metadata
  logoUrl: text('logo_url'),
  primaryColor: varchar('primary_color', { length: 7 }),

  createdAt: timestamp('created_at').defaultNow().notNull(),
  updatedAt: timestamp('updated_at').defaultNow().notNull()
}, (table) => {
  return {
    codeIdx: uniqueIndex('banks_code_idx').on(table.code),
    activeIdx: index('banks_active_idx').on(table.isActive)
  }
}).enableRLS()

// Message templates table
export const messageTemplates = pgTable('message_templates', {
  id: uuid('id').defaultRandom().primaryKey(),
  bankId: uuid('bank_id').references(() => banks.id, { onDelete: 'cascade' }).notNull(),

  // Template classification
  templateCode: varchar('template_code', { length: 50 }).notNull(),
  templateName: varchar('template_name', { length: 100 }).notNull(),
  templateCategory: messageCategoryEnum('template_category').notNull(),

  // Transaction mapping
  transactionType: transactionTypeEnum('transaction_type'),

  // Example and documentation
  exampleMessage: text('example_message').notNull(),
  description: text('description'),
  notes: text('notes'),

  // Fields this template contains
  hasAmount: boolean('has_amount').default(false).notNull(),
  hasMerchant: boolean('has_merchant').default(false).notNull(),
  hasBalance: boolean('has_balance').default(false).notNull(),
  hasReference: boolean('has_reference').default(false).notNull(),
  hasAccount: boolean('has_account').default(false).notNull(),
  hasCard: boolean('has_card').default(false).notNull(),
  hasUpiId: boolean('has_upi_id').default(false).notNull(),

  // Pattern matching
  identifyingKeywords: text('identifying_keywords').array(),
  excludeKeywords: text('exclude_keywords').array(),
  regexPattern: text('regex_pattern'),

  // Stats
  usageCount: integer('usage_count').default(0).notNull(),
  successRate: decimal('success_rate', { precision: 5, scale: 2 }),

  // Status
  isActive: boolean('is_active').default(true).notNull(),
  isVerified: boolean('is_verified').default(false).notNull(),

  createdAt: timestamp('created_at').defaultNow().notNull(),
  updatedAt: timestamp('updated_at').defaultNow().notNull()
}, (table) => {
  return {
    bankTemplateIdx: index('template_bank_idx').on(table.bankId),
    categoryIdx: index('template_category_idx').on(table.templateCategory),
    uniqueTemplateCode: uniqueIndex('unique_template_code').on(table.bankId, table.templateCode)
  }
}).enableRLS()

// Categories table
export const categories = pgTable('categories', {
  id: uuid('id').defaultRandom().primaryKey(),
  code: varchar('code', { length: 50 }).unique().notNull(),
  name: varchar('name', { length: 100 }).notNull(),

  // Visual
  icon: varchar('icon', { length: 20 }),
  color: varchar('color', { length: 7 }),

  // Hierarchy
  parentCategoryId: uuid('parent_category_id'),
  displayOrder: integer('display_order').default(0).notNull(),

  // Matching rules
  keywords: jsonb('keywords').$type<string[]>().default([]),
  merchantPatterns: text('merchant_patterns').array(),

  // Stats
  transactionCount: integer('transaction_count').default(0).notNull(),
  totalAmount: decimal('total_amount', { precision: 15, scale: 2 }).default('0'),

  isActive: boolean('is_active').default(true).notNull(),
  createdAt: timestamp('created_at').defaultNow().notNull()
}, (table) => {
  return {
    // Self-referential FK must be declared at table-level to avoid self-reference in initializer
    categoriesParentFk: foreignKey({
      name: 'categories_parent_fk',
      columns: [table.parentCategoryId],
      foreignColumns: [table.id]
    }).onDelete('set null').onUpdate('cascade'),
    codeIdx: uniqueIndex('categories_code_idx').on(table.code),
    activeIdx: index('categories_active_idx').on(table.isActive)
  }
}).enableRLS()

// Submissions table
export const submissions = pgTable('submissions', {
  id: uuid('id').defaultRandom().primaryKey(),

  // Core fields
  smsBody: text('sms_body').notNull(),
  sender: varchar('sender', { length: 50 }).notNull(),

  // Relations
  detectedBankId: uuid('detected_bank_id').references(() => banks.id),
  matchedTemplateId: uuid('matched_template_id').references(() => messageTemplates.id),

  // Parsing results
  isSupported: boolean('is_supported').default(false).notNull(),
  isParseable: boolean('is_parseable').default(false).notNull(),
  confidenceScore: decimal('confidence_score', { precision: 3, scale: 2 }),

  parsedData: jsonb('parsed_data').$type<{
    amount?: number
    type?: 'INCOME' | 'EXPENSE'
    merchant?: string
    category?: string
    reference?: string
    account?: string
    balance?: number
  }>(),

  parseErrors: jsonb('parse_errors').$type<{
    field?: string
    error?: string
    details?: any
  }[]>(),

  // User input
  userExpected: jsonb('user_expected').$type<{
    amount?: number
    type?: 'INCOME' | 'EXPENSE'
    merchant?: string
    category?: string
    isTransaction?: boolean
  }>(),

  // Message format
  messageFormat: varchar('message_format', { length: 50 }),

  // Status
  status: submissionStatusEnum('status').default('pending').notNull(),
  priority: integer('priority').default(0).notNull(),

  // Metadata
  sessionId: varchar('session_id', { length: 100 }),
  ipHash: varchar('ip_hash', { length: 64 }),
  ipCountry: varchar('ip_country', { length: 2 }),
  ipRegion: varchar('ip_region', { length: 100 }),
  userAgent: text('user_agent'),

  // Admin
  reviewedAt: timestamp('reviewed_at'),
  reviewedBy: varchar('reviewed_by', { length: 100 }),
  adminNotes: text('admin_notes'),

  createdAt: timestamp('created_at').defaultNow().notNull(),
  updatedAt: timestamp('updated_at').defaultNow().notNull()
}, (table) => {
  return {
    statusIdx: index('submissions_status_idx').on(table.status),
    senderIdx: index('submissions_sender_idx').on(table.sender),
    bankIdx: index('submissions_bank_idx').on(table.detectedBankId),
    createdIdx: index('submissions_created_idx').on(table.createdAt.desc()),
    sessionIdx: index('submissions_session_idx').on(table.sessionId)
  }
}).enableRLS()

// Feedback table
export const feedback = pgTable('feedback', {
  id: uuid('id').defaultRandom().primaryKey(),
  submissionId: uuid('submission_id').references(() => submissions.id, { onDelete: 'cascade' }).notNull(),

  feedbackType: varchar('feedback_type', { length: 50 }).notNull(),
  correctValue: text('correct_value'),
  explanation: text('explanation'),

  // User contact (optional)
  contactEmail: varchar('contact_email', { length: 255 }),

  // Status
  isAddressed: boolean('is_addressed').default(false).notNull(),
  addressedInVersion: integer('addressed_in_version'),

  sessionId: varchar('session_id', { length: 100 }),
  createdAt: timestamp('created_at').defaultNow().notNull()
}, (table) => {
  return {
    submissionIdx: index('feedback_submission_idx').on(table.submissionId)
  }
}).enableRLS()

// Parser patterns table (for version control)
export const parserPatterns = pgTable('parser_patterns', {
  id: uuid('id').defaultRandom().primaryKey(),
  bankId: uuid('bank_id').references(() => banks.id, { onDelete: 'cascade' }).notNull(),

  // Version control
  version: integer('version').notNull(),
  isCurrent: boolean('is_current').default(false).notNull(),

  // Pattern configuration
  patterns: jsonb('patterns').$type<Record<string, any>>().notNull(),

  // Testing
  testCases: jsonb('test_cases').$type<Array<{
    sms: string
    expected: any
  }>>(),
  testPassRate: decimal('test_pass_rate', { precision: 5, scale: 2 }),

  // Deployment
  deployedAt: timestamp('deployed_at'),
  deployedBy: varchar('deployed_by', { length: 100 }),
  rollbackToVersion: integer('rollback_to_version'),

  createdAt: timestamp('created_at').defaultNow().notNull(),
  notes: text('notes')
}, (table) => {
  return {
    bankVersionIdx: uniqueIndex('bank_version_idx').on(table.bankId, table.version),
    currentIdx: index('current_patterns_idx').on(table.bankId)
  }
})

// Daily analytics table
export const dailyAnalytics = pgTable('daily_analytics', {
  id: uuid('id').defaultRandom().primaryKey(),
  date: timestamp('date').notNull().unique(),

  // Submission metrics
  totalSubmissions: integer('total_submissions').default(0).notNull(),
  successfulParses: integer('successful_parses').default(0).notNull(),
  failedParses: integer('failed_parses').default(0).notNull(),
  uniqueSessions: integer('unique_sessions').default(0).notNull(),

  // Bank breakdown
  bankStats: jsonb('bank_stats').$type<Record<string, {
    submissions: number
    success: number
  }>>(),

  // Template usage
  templateStats: jsonb('template_stats').$type<Record<string, {
    count: number
    successRate: number
  }>>(),

  // Geographic
  countryStats: jsonb('country_stats').$type<Record<string, number>>(),

  // Top failures
  topFailedSenders: text('top_failed_senders').array(),
  topFailedPatterns: text('top_failed_patterns').array(),

  createdAt: timestamp('created_at').defaultNow().notNull()
})

// Type exports for use in the app
export type Bank = typeof banks.$inferSelect
export type NewBank = typeof banks.$inferInsert
export type MessageTemplate = typeof messageTemplates.$inferSelect
export type NewMessageTemplate = typeof messageTemplates.$inferInsert
export type Category = typeof categories.$inferSelect
export type NewCategory = typeof categories.$inferInsert
export type Submission = typeof submissions.$inferSelect
export type NewSubmission = typeof submissions.$inferInsert
export type Feedback = typeof feedback.$inferSelect
export type NewFeedback = typeof feedback.$inferInsert
export type ParserPattern = typeof parserPatterns.$inferSelect
export type NewParserPattern = typeof parserPatterns.$inferInsert
export type DailyAnalytics = typeof dailyAnalytics.$inferSelect
export type NewDailyAnalytics = typeof dailyAnalytics.$inferInsert
