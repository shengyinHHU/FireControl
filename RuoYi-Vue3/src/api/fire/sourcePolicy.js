// Once a resource has a database owner, demo rows must never be merged or used as fallback.
export const DATABASE_FIRE_RESOURCES = Object.freeze(['places', 'cameras', 'inspections', 'gateways', 'alarms', 'protocolMessages', 'sms'])
export const usesDatabase = resource => DATABASE_FIRE_RESOURCES.includes(resource)
// Places have explicit real CRUD endpoints. Other database resources use their dedicated owners.
export const canWriteResource = resource => resource === 'places' || !usesDatabase(resource)
