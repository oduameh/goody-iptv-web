# 🔐 Security Guide - Goody IPTV

## 🚨 CRITICAL: API Key Security

### **NEVER commit these files to git:**
- `.env.local` (your actual API keys)
- `config.js` (client-side config with keys)
- Any file containing real API keys or secrets

### **Files that ARE safe to commit:**
- `env.example` (template with fake keys)
- `config.example.js` (template with fake keys)
- `SECURITY-GUIDE.md` (this file)

## 🔑 API Key Management

### **Step 1: Set Up Environment Variables**

1. **Copy the template:**
   ```bash
   cp env.example .env.local
   ```

2. **Fill in your real API keys in `.env.local`:**
   ```bash
   # Stripe Keys (from https://dashboard.stripe.com/apikeys)
   NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY=pk_test_51ABC123...your_real_key
   STRIPE_SECRET_KEY=sk_test_51ABC123...your_real_secret_key
   STRIPE_WEBHOOK_SECRET=whsec_ABC123...your_webhook_secret
   
   # Email Service (from https://resend.com/api-keys)
   RESEND_API_KEY=re_ABC123...your_real_api_key
   FROM_EMAIL=noreply@yourdomain.com
   ```

### **Step 2: Set Up Client-Side Config**

1. **Copy the template:**
   ```bash
   cp config.example.js config.js
   ```

2. **Fill in your public keys in `config.js`:**
   ```javascript
   window.GOODY_CONFIG = {
     STRIPE_PUBLIC_KEY: 'pk_test_51ABC123...your_real_publishable_key',
     STRIPE_PAYMENT_LINK: 'https://buy.stripe.com/ABC123...your_real_link',
     SUPPORT_EMAIL: 'support@yourdomain.com',
     // ... other settings
   };
   ```

## 🛡️ Security Best Practices

### **Environment Variable Security:**

1. **Server-side only (NEVER expose):**
   - `STRIPE_SECRET_KEY` (starts with `sk_`)
   - `STRIPE_WEBHOOK_SECRET` (starts with `whsec_`)
   - `RESEND_API_KEY`
   - Database credentials

2. **Client-side safe (can expose):**
   - `STRIPE_PUBLISHABLE_KEY` (starts with `pk_`)
   - `GA_MEASUREMENT_ID`
   - Domain names and public URLs

### **Webhook Security:**
- ✅ Always verify Stripe webhook signatures
- ✅ Use HTTPS endpoints only
- ✅ Log all webhook events for debugging
- ✅ Handle webhook retries gracefully

### **License Key Security:**
- ✅ Bind licenses to device IDs
- ✅ Use cryptographic hashing for key generation
- ✅ Set expiration dates (180 days)
- ✅ Store licenses in secure database

## 🔄 Development vs Production

### **Development (Test Mode):**
```bash
# Use test keys (safe for development)
STRIPE_SECRET_KEY=sk_test_...
NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY=pk_test_...
NODE_ENV=development
```

### **Production (Live Mode):**
```bash
# Use live keys (KEEP SECRET!)
STRIPE_SECRET_KEY=sk_live_...
NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY=pk_live_...
NODE_ENV=production
```

## 🚀 Deployment Security

### **Vercel Deployment:**

1. **Set environment variables in Vercel dashboard:**
   - Go to Project Settings > Environment Variables
   - Add all variables from `.env.local`
   - Set different values for Preview vs Production

2. **Never put secrets in `vercel.json` or other config files**

### **GitHub Actions Security:**
- Use GitHub Secrets for API keys
- Never log secret values
- Use separate keys for CI/CD

## 🔍 Security Checklist

### **Before Going Live:**

- [ ] All test keys replaced with live keys
- [ ] Webhook endpoint using HTTPS
- [ ] Webhook signature verification enabled
- [ ] Database access properly secured
- [ ] Email service configured and verified
- [ ] No API keys in git history
- [ ] `.env.local` and `config.js` in `.gitignore`
- [ ] Error messages don't expose sensitive data
- [ ] CORS properly configured
- [ ] Rate limiting implemented

### **Monitoring:**
- [ ] Set up error tracking (Sentry)
- [ ] Monitor webhook failures
- [ ] Track failed payment attempts
- [ ] Log all license generations
- [ ] Monitor for suspicious activity

## 🚨 What to Do If Keys Are Compromised

### **If API keys are accidentally committed:**

1. **Immediately rotate the keys:**
   - Go to Stripe Dashboard > API Keys > Create new key
   - Update your `.env.local` and deployment config
   - Delete the old key

2. **Remove from git history:**
   ```bash
   # Remove sensitive file from git history
   git filter-branch --force --index-filter \
   'git rm --cached --ignore-unmatch .env.local' \
   --prune-empty --tag-name-filter cat -- --all
   
   # Force push to update remote
   git push origin --force --all
   ```

3. **Check Stripe dashboard for unauthorized transactions**

### **Emergency Response:**
- Disable webhook endpoints
- Revoke all API keys
- Check recent transactions
- Contact Stripe support if needed

## 📧 Secure Email Templates

### **License delivery email should:**
- ✅ Come from verified domain
- ✅ Include license expiration date
- ✅ Provide clear activation instructions
- ✅ Include support contact information
- ❌ Never include API keys or secrets

## 🔐 Database Security

### **Supabase Security:**
```sql
-- Enable Row Level Security
ALTER TABLE licenses ENABLE ROW LEVEL SECURITY;

-- Policy: Users can only see their own licenses
CREATE POLICY "Users can view own licenses" ON licenses
    FOR SELECT USING (auth.uid() = user_id);
```

### **License Storage:**
```javascript
// Store licenses securely
const licenseData = {
  device_id: deviceId,           // Hashed device identifier
  license_key: hashedKey,        // Don't store plain text
  customer_email: email,         // For support purposes
  expires_at: expirationDate,    // Clear expiration
  created_at: new Date(),        // Audit trail
  stripe_session_id: sessionId   // For refund/support
};
```

## 🎯 Summary

**Golden Rules:**
1. **NEVER commit real API keys to git**
2. **Use environment variables for all secrets**
3. **Verify all webhook signatures**
4. **Monitor for security issues**
5. **Rotate keys if compromised**

**Your setup is secure when:**
- ✅ API keys are in environment variables only
- ✅ Webhooks are verified and use HTTPS
- ✅ No secrets in git repository
- ✅ Production uses live Stripe keys
- ✅ Error monitoring is active

**Ready for secure payments!** 💰🔒 