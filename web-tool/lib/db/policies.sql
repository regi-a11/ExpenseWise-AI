-- Row Level Security Policies for PennyWise SMS Parser
-- Run this after tables are created

-- ============================================
-- SUBMISSIONS TABLE POLICIES
-- ============================================

-- Allow anyone to insert submissions
CREATE POLICY "public_insert_submissions" ON submissions
  FOR INSERT 
  WITH CHECK (true);

-- No public reads (privacy protection)
CREATE POLICY "no_public_read_submissions" ON submissions
  FOR SELECT 
  USING (false);

-- Rate limiting: max 10 submissions per hour per IP
CREATE POLICY "rate_limit_submissions" ON submissions
  FOR INSERT 
  WITH CHECK (
    (SELECT COUNT(*) 
     FROM submissions 
     WHERE ip_hash = NEW.ip_hash 
       AND created_at > NOW() - INTERVAL '1 hour'
    ) < 10
  );

-- ============================================
-- BANKS TABLE POLICIES
-- ============================================

-- Public can read active banks only
CREATE POLICY "public_read_active_banks" ON banks
  FOR SELECT 
  USING (is_active = true);

-- ============================================
-- MESSAGE TEMPLATES TABLE POLICIES
-- ============================================

-- Public can read active templates only
CREATE POLICY "public_read_active_templates" ON message_templates
  FOR SELECT 
  USING (is_active = true);

-- ============================================
-- CATEGORIES TABLE POLICIES
-- ============================================

-- Public can read all active categories
CREATE POLICY "public_read_active_categories" ON categories
  FOR SELECT 
  USING (is_active = true);

-- ============================================
-- FEEDBACK TABLE POLICIES
-- ============================================

-- Anyone can insert feedback
CREATE POLICY "public_insert_feedback" ON feedback
  FOR INSERT 
  WITH CHECK (true);

-- Users can only read their own feedback (by session_id)
CREATE POLICY "read_own_feedback" ON feedback
  FOR SELECT 
  USING (session_id = current_setting('app.session_id', true));

-- ============================================
-- PARSER PATTERNS TABLE POLICIES
-- ============================================

-- Public can read current patterns only
CREATE POLICY "public_read_current_patterns" ON parser_patterns
  FOR SELECT 
  USING (is_current = true);

-- ============================================
-- DAILY ANALYTICS TABLE POLICIES
-- ============================================

-- Public can read aggregated analytics
CREATE POLICY "public_read_analytics" ON daily_analytics
  FOR SELECT 
  USING (true);