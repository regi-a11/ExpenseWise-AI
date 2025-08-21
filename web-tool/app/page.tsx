'use client'

import { useState, useEffect, Suspense } from 'react'
import { BankParserFactory } from '@/lib/parsers'
import { CategoryMapper } from '@/lib/categorization'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import { SubmissionDialog } from '@/components/submission-dialog'
import Link from 'next/link'
import { BookOpen, Github } from 'lucide-react'

function HomeContent() {
  const [smsBody, setSmsBody] = useState('')
  const [sender, setSender] = useState('')
  const [parseResult, setParseResult] = useState<any>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [showSubmissionDialog, setShowSubmissionDialog] = useState(false)
  const [submissionMode, setSubmissionMode] = useState<'submit' | 'improve'>('submit')
  const [hasAutoProcessed, setHasAutoProcessed] = useState(false)
  const [encryptedDeviceData, setEncryptedDeviceData] = useState<string | null>(null)

  const handleParse = () => {
    setIsLoading(true)
    
    try {
      const result = BankParserFactory.parse(smsBody, sender, Date.now())
      setParseResult(result)
    } catch (error) {
      setParseResult({
        success: false,
        error: 'An error occurred while parsing the SMS'
      })
    }
    
    setIsLoading(false)
  }

  const handleClear = () => {
    setSmsBody('')
    setSender('')
    setParseResult(null)
  }

  const handleOpenSubmission = (mode: 'submit' | 'improve') => {
    setSubmissionMode(mode)
    setShowSubmissionDialog(true)
  }

  // Handle URL hash parameters on mount
  useEffect(() => {
    if (hasAutoProcessed) return // Prevent re-running
    
    // Parse hash fragment instead of query params
    const hash = window.location.hash
    if (!hash || hash.length <= 1) return
    
    // Remove the # and parse the parameters
    const hashParams = new URLSearchParams(hash.slice(1))
    const message = hashParams.get('message')
    const senderId = hashParams.get('sender')
    const deviceData = hashParams.get('device')
    const autoParse = hashParams.get('autoparse')
    
    if (message) {
      const decodedMessage = decodeURIComponent(message)
      setSmsBody(decodedMessage)
    }
    
    if (senderId) {
      const decodedSender = decodeURIComponent(senderId)
      setSender(decodedSender)
    }
    
    if (deviceData) {
      const decodedDeviceData = decodeURIComponent(deviceData)
      console.log('Received device data from hash:', { 
        raw: deviceData, 
        decoded: decodedDeviceData,
        length: decodedDeviceData.length 
      })
      setEncryptedDeviceData(decodedDeviceData)
    } else {
      console.log('No device data in hash parameters')
    }
    
    // Clear hash for privacy - remove sensitive data from URL
    if (message || senderId || deviceData) {
      // Use replaceState to avoid adding to browser history
      window.history.replaceState(null, '', window.location.pathname)
    }
    
    // Auto-parse if requested and both message and sender are provided
    if (autoParse === 'true' && message && senderId) {
      setHasAutoProcessed(true)
      // Delay parsing slightly to ensure state is set
      setTimeout(() => {
        const decodedMessage = decodeURIComponent(message)
        const decodedSender = decodeURIComponent(senderId)
        
        try {
          const result = BankParserFactory.parse(decodedMessage, decodedSender, Date.now())
          setParseResult(result)
        } catch (error) {
          setParseResult({
            success: false,
            error: 'An error occurred while parsing the SMS'
          })
        }
      }, 100)
    }
  }, [hasAutoProcessed])


  return (
    <div className="container mx-auto py-8 px-4 max-w-4xl">
      <div className="space-y-6">
        <div className="flex justify-between items-start">
          <div>
            <h1 className="text-3xl font-bold">PennyWise SMS Parser</h1>
            <p className="text-muted-foreground mt-2">
              Test if your bank SMS messages are compatible with PennyWise
            </p>
          </div>
          <div className="flex gap-2">
            <Link href="/bank-support">
              <Button variant="outline" className="gap-2">
                <BookOpen className="h-4 w-4" />
                Bank Support Docs
              </Button>
            </Link>
            <Link href="https://github.com/sarim2000/pennywiseai-tracker" target="_blank" rel="noopener noreferrer">
              <Button variant="outline" className="gap-2">
                <Github className="h-4 w-4" />
                GitHub
              </Button>
            </Link>
          </div>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>Test Your SMS</CardTitle>
            <CardDescription>
              Paste your bank SMS and sender ID to check if it can be parsed
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="sender">Sender ID</Label>
              <Input
                id="sender"
                placeholder="e.g., HDFCBK, SBIBNK"
                value={sender}
                onChange={(e) => setSender(e.target.value)}
              />
              <p className="text-xs text-muted-foreground">
                The sender ID shown in your SMS app (usually 6 characters)
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="sms">SMS Message</Label>
              <Textarea
                id="sms"
                placeholder="Paste your bank transaction SMS here..."
                value={smsBody}
                onChange={(e) => setSmsBody(e.target.value)}
                rows={6}
              />
              <p className="text-xs text-muted-foreground">
                ⚠️ Please randomize your private information (account numbers, card numbers, UPI IDs, etc.) before submitting
              </p>
            </div>

            <div className="flex gap-2">
              <Button 
                onClick={handleParse} 
                disabled={!smsBody || !sender || isLoading}
              >
                Parse SMS
              </Button>
              <Button 
                variant="outline" 
                onClick={handleClear}
              >
                Clear
              </Button>
            </div>
          </CardContent>
        </Card>

        {parseResult && (
          <Card>
            <CardHeader>
              <CardTitle>
                {parseResult.success ? 'Parsed Successfully' : 'Parsing Failed'}
              </CardTitle>
            </CardHeader>
            <CardContent>
              {parseResult.success ? (
                <div className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <p className="text-sm font-medium">Amount</p>
                      <p className="text-2xl font-bold">
                        ₹{parseResult.data.amount?.toLocaleString('en-IN')}
                      </p>
                    </div>
                    <div>
                      <p className="text-sm font-medium">Type</p>
                      <Badge variant={parseResult.data.type === 'EXPENSE' ? 'destructive' : 'default'}>
                        {parseResult.data.type}
                      </Badge>
                    </div>
                  </div>

                  <Separator />

                  <div className="space-y-2">
                    {parseResult.data.merchant && (
                      <div className="flex justify-between">
                        <span className="text-sm font-medium">Merchant</span>
                        <span className="text-sm">{parseResult.data.merchant}</span>
                      </div>
                    )}
                    
                    {parseResult.data.category && (
                      <div className="flex justify-between">
                        <span className="text-sm font-medium">Category</span>
                        <Badge variant="outline">
                          {parseResult.data.category}
                        </Badge>
                      </div>
                    )}

                    {parseResult.data.bankName && (
                      <div className="flex justify-between">
                        <span className="text-sm font-medium">Bank</span>
                        <span className="text-sm">{parseResult.data.bankName}</span>
                      </div>
                    )}

                    {parseResult.data.accountLast4 && (
                      <div className="flex justify-between">
                        <span className="text-sm font-medium">Account</span>
                        <span className="text-sm">****{parseResult.data.accountLast4}</span>
                      </div>
                    )}

                    {parseResult.data.balance !== null && (
                      <div className="flex justify-between">
                        <span className="text-sm font-medium">Balance</span>
                        <span className="text-sm">₹{parseResult.data.balance.toLocaleString('en-IN')}</span>
                      </div>
                    )}

                    {parseResult.data.reference && (
                      <div className="flex justify-between">
                        <span className="text-sm font-medium">Reference</span>
                        <span className="text-sm font-mono text-xs">{parseResult.data.reference}</span>
                      </div>
                    )}
                  </div>

                  {parseResult.confidence && (
                    <>
                      <Separator />
                      <div className="flex justify-between">
                        <span className="text-sm font-medium">Confidence</span>
                        <span className="text-sm">{(parseResult.confidence * 100).toFixed(0)}%</span>
                      </div>
                    </>
                  )}

                  <Separator />
                  
                  <Button 
                    onClick={() => handleOpenSubmission('improve')}
                    variant="outline"
                    className="w-full"
                  >
                    Improve Results
                  </Button>
                </div>
              ) : (
                <div className="space-y-4">
                  <Alert>
                    <AlertDescription>
                      {parseResult.error || 'Unable to parse this SMS. The bank or format may not be supported yet.'}
                    </AlertDescription>
                  </Alert>
                  <Button 
                    onClick={() => handleOpenSubmission('submit')}
                    className="w-full"
                    variant="outline"
                  >
                    Submit Correct Details
                  </Button>
                </div>
              )}
            </CardContent>
          </Card>
        )}


        <Card>
          <CardHeader>
            <CardTitle>Report Unsupported SMS</CardTitle>
            <CardDescription>
              If your bank SMS isn't parsing correctly, you can submit it for review
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Button 
              variant="outline" 
              className="w-full"
              onClick={() => handleOpenSubmission('submit')}
              disabled={!smsBody || !sender}
            >
              Submit Unsupported SMS
            </Button>
          </CardContent>
        </Card>
      </div>

      <SubmissionDialog
        open={showSubmissionDialog}
        onOpenChange={setShowSubmissionDialog}
        smsBody={smsBody}
        sender={sender}
        parseResult={parseResult}
        mode={submissionMode}
        encryptedDeviceData={encryptedDeviceData}
      />
    </div>
  )
}

export default function Home() {
  return (
    <Suspense fallback={
      <div className="container mx-auto py-8 px-4 max-w-4xl">
        <div className="text-center">Loading...</div>
      </div>
    }>
      <HomeContent />
    </Suspense>
  )
}