'use client'

import { useState, useEffect } from 'react'
import { createClient } from '@supabase/supabase-js'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Checkbox } from '@/components/ui/checkbox'
import { CategoryMapper } from '@/lib/categorization'

interface SubmissionDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  smsBody: string
  sender: string
  parseResult?: any
  mode?: 'submit' | 'improve'
  encryptedDeviceData?: string | null
}

export function SubmissionDialog({ 
  open, 
  onOpenChange, 
  smsBody, 
  sender,
  parseResult,
  mode = 'submit',
  encryptedDeviceData
}: SubmissionDialogProps) {
  const [submitting, setSubmitting] = useState(false)
  const [success, setSuccess] = useState(false)
  const [error, setError] = useState('')
  
  // Manual correction fields
  const [amount, setAmount] = useState('')
  const [merchant, setMerchant] = useState('')
  const [transactionType, setTransactionType] = useState<'INCOME' | 'EXPENSE'>('EXPENSE')
  const [category, setCategory] = useState('')
  const [bankName, setBankName] = useState('')
  const [additionalNotes, setAdditionalNotes] = useState('')
  const [shouldIgnore, setShouldIgnore] = useState(false)

  // Pre-fill form when dialog opens in improve mode
  useEffect(() => {
    if (open && mode === 'improve' && parseResult?.success && parseResult?.data) {
      setAmount(parseResult.data.amount?.toString() || '')
      setMerchant(parseResult.data.merchant || '')
      setTransactionType(parseResult.data.type || 'EXPENSE')
      setCategory(parseResult.data.category || '')
      setBankName(parseResult.data.bankName || '')
      setShouldIgnore(false)
    } else if (open && mode === 'submit') {
      // Reset form for submit mode
      setAmount('')
      setMerchant('')
      setTransactionType('EXPENSE')
      setCategory('')
      setBankName('')
      setShouldIgnore(false)
    }
  }, [open, mode, parseResult])

  const categories = CategoryMapper.getExpenseCategories()

  const handleSubmit = async () => {
    setSubmitting(true)
    setError('')
    
    try {
      // Check if we have encrypted device data (from Android app)
      // If not, this is from web-only usage which we'll allow but flag differently
      if (!encryptedDeviceData) {
        console.log('No device verification available - web submission')
      }

      // Prepare the request payload
      const payload = {
        smsBody: smsBody,
        sender: sender,
        encryptedDeviceData: encryptedDeviceData || '', // Empty string if from web
        parsedData: parseResult?.success ? {
          amount: parseResult.data.amount,
          type: parseResult.data.type,
          merchant: parseResult.data.merchant,
          category: parseResult.data.category,
          reference: parseResult.data.reference,
          accountLast4: parseResult.data.accountLast4,
          balance: parseResult.data.balance
        } : undefined,
        userExpected: {
          amount: shouldIgnore ? null : (amount ? parseFloat(amount) : null),
          merchant: shouldIgnore ? null : (merchant || null),
          category: shouldIgnore ? null : (category || null),
          type: shouldIgnore ? null : transactionType,
          isTransaction: !shouldIgnore
        },
        additionalNotes: additionalNotes || null,
        mode: mode
      }

      // Call the Edge Function
      const response = await fetch(`${process.env.NEXT_PUBLIC_SUPABASE_URL}/functions/v1/submit-sms`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY}`
        },
        body: JSON.stringify(payload)
      })

      const result = await response.json()

      if (!response.ok) {
        throw new Error(result.error || 'Failed to submit')
      }

      setSuccess(true)
      setTimeout(() => {
        onOpenChange(false)
        // Reset form
        setAmount('')
        setMerchant('')
        setCategory('')
        setBankName('')
        setAdditionalNotes('')
        setSuccess(false)
      }, 2000)
    } catch (err: any) {
      setError(err.message || 'Failed to submit')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>
            {mode === 'improve' ? 'Improve Parsed Results' : 'Submit Unsupported SMS'}
          </DialogTitle>
          <DialogDescription>
            {mode === 'improve' 
              ? 'Help us improve accuracy by correcting any incorrect details'
              : 'Help us improve by providing the correct transaction details'}
          </DialogDescription>
        </DialogHeader>

        {success ? (
          <Alert>
            <AlertDescription>
              Thank you! Your submission has been received and will help improve the parser.
            </AlertDescription>
          </Alert>
        ) : (
          <>
            <div className="space-y-4">
              {/* Show SMS preview */}
              <div className="space-y-2">
                <Label>SMS Message</Label>
                <div className="p-3 rounded-md border text-sm">
                  <div className="font-medium">From: {sender}</div>
                  <div className="mt-1 text-muted-foreground">{smsBody}</div>
                </div>
              </div>

              {/* Ignore checkbox */}
              <div className="flex items-center space-x-2 p-4 rounded-lg border">
                <Checkbox 
                  id="ignore" 
                  checked={shouldIgnore}
                  onCheckedChange={(checked) => setShouldIgnore(checked as boolean)}
                />
                <div className="flex-1">
                  <Label htmlFor="ignore" className="text-sm font-medium cursor-pointer">
                    This is not a transaction
                  </Label>
                  <p className="text-xs text-muted-foreground">
                    Check this for OTPs, promotional messages, or other non-transaction SMS
                  </p>
                </div>
              </div>

              {/* Manual correction fields */}
              {!shouldIgnore && (
                <>
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="amount">Amount</Label>
                      <Input
                        id="amount"
                        type="number"
                        placeholder="1000.00"
                        value={amount}
                        onChange={(e) => setAmount(e.target.value)}
                      />
                      {mode === 'improve' && parseResult?.data?.amount && (
                        <p className="text-xs text-muted-foreground">
                          Original: ₹{parseResult.data.amount.toLocaleString('en-IN')}
                        </p>
                      )}
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="type">Transaction Type</Label>
                      <Select value={transactionType} onValueChange={(v) => setTransactionType(v as 'INCOME' | 'EXPENSE')}>
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="EXPENSE">Expense</SelectItem>
                          <SelectItem value="INCOME">Income</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="merchant">Merchant/Description</Label>
                    <Input
                      id="merchant"
                      placeholder="e.g., Swiggy, Amazon, Salary"
                      value={merchant}
                      onChange={(e) => setMerchant(e.target.value)}
                    />
                    {mode === 'improve' && parseResult?.data?.merchant && (
                      <p className="text-xs text-muted-foreground">
                        Original: {parseResult.data.merchant}
                      </p>
                    )}
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="category">Category</Label>
                      <Select value={category} onValueChange={setCategory}>
                        <SelectTrigger>
                          <SelectValue placeholder="Select category" />
                        </SelectTrigger>
                        <SelectContent>
                          {categories.map((cat) => (
                            <SelectItem key={cat.code} value={cat.code}>
                              <span className="flex items-center gap-2">
                                <span>{cat.icon}</span>
                                <span>{cat.displayName}</span>
                              </span>
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="bank">Bank Name</Label>
                      <Input
                        id="bank"
                        placeholder="e.g., HDFC Bank"
                        value={bankName}
                        onChange={(e) => setBankName(e.target.value)}
                      />
                    </div>
                  </div>
                </>
              )}

              <div className="space-y-2">
                <Label htmlFor="notes">Additional Notes (Optional)</Label>
                <Textarea
                  id="notes"
                  placeholder="Any additional information that might help..."
                  value={additionalNotes}
                  onChange={(e) => setAdditionalNotes(e.target.value)}
                  rows={3}
                />
              </div>

              {error && (
                <Alert variant="destructive">
                  <AlertDescription>{error}</AlertDescription>
                </Alert>
              )}
            </div>

            <DialogFooter>
              <Button variant="outline" onClick={() => onOpenChange(false)}>
                Cancel
              </Button>
              <Button 
                onClick={handleSubmit} 
                disabled={submitting || (!shouldIgnore && (!amount || !merchant))}
              >
                {submitting ? 'Submitting...' : 'Submit'}
              </Button>
            </DialogFooter>
          </>
        )}
      </DialogContent>
    </Dialog>
  )
}