/**
 * Abstract Base Model for all QuickBite domain entities.
 * Encapsulates common attributes, serialization, and immutability helpers.
 */
export class BaseModel {
  constructor(data = {}) {
    this.id = data.id !== undefined && data.id !== null ? Number(data.id) : null;
    this.createdAt = data.createdAt ? new Date(data.createdAt) : new Date();
    this.updatedAt = data.updatedAt ? new Date(data.updatedAt) : new Date();
  }

  getId() {
    return this.id;
  }

  isPersisted() {
    return this.id !== null && !isNaN(this.id);
  }

  equals(other) {
    if (!other || !(other instanceof BaseModel)) return false;
    if (!this.id || !other.id) return this === other;
    return this.id === other.id && this.constructor.name === other.constructor.name;
  }

  toJSON() {
    return { ...this };
  }
}
