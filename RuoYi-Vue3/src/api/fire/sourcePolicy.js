// Once a resource has a database owner, demo rows must never be merged or used as fallback.
export const DATABASE_FIRE_RESOURCES = Object.freeze(['places'])
export const usesDatabase = resource => DATABASE_FIRE_RESOURCES.includes(resource)
// Place writes are intentionally unavailable until real CRUD endpoints are implemented.
export const canWriteResource = resource => !usesDatabase(resource)
