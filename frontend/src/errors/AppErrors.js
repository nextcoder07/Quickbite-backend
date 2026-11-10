/**
 * QuickBite Domain & Application Exception Hierarchy
 * Follows OOP inheritance to represent specific categories of failures.
 */

export class AppError extends Error {
  constructor(message, code = 'APP_ERROR') {
    super(message);
    this.name = this.constructor.name;
    this.code = code;
    this.timestamp = new Date();
    if (Error.captureStackTrace) {
      Error.captureStackTrace(this, this.constructor);
    }
  }
}

export class HttpError extends AppError {
  constructor(message, status = 500, data = null) {
    super(message, `HTTP_${status}`);
    this.status = status;
    this.data = data;
  }
}

export class ValidationError extends AppError {
  constructor(message, fieldErrors = {}) {
    super(message, 'VALIDATION_ERROR');
    this.fieldErrors = fieldErrors;
  }
}

export class AuthenticationError extends AppError {
  constructor(message = 'Session expired or not authenticated') {
    super(message, 'AUTH_ERROR');
    this.status = 401;
  }
}

export class AuthorizationError extends AppError {
  constructor(message = 'You do not have permission to perform this action') {
    super(message, 'FORBIDDEN');
    this.status = 403;
  }
}

export class BusinessRuleViolationError extends AppError {
  constructor(message) {
    super(message, 'BUSINESS_RULE_VIOLATION');
  }
}
