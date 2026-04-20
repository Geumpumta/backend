-- Department ranking: full ZSET scan for each department + active session merge +
-- top-30 aggregate per department + final department ordering.
-- KEYS[1] = active:users set
-- KEYS[2..N+1] = N department ZSETs
-- ARGV[1] = now (epoch ms)
-- ARGV[2] = numDepts (N)
-- ARGV[3..2+N] = department names (aligned with KEYS[2..1+N])
--
-- Return (flat array): deptCount, (deptName, totalMillis) * deptCount

local activeUsersKey = KEYS[1]
local now = tonumber(ARGV[1])
local numDepts = tonumber(ARGV[2])

-- Pre-bucket active users by their department
local activeByDept = {}
local activeUsers = redis.call('SMEMBERS', activeUsersKey)
for _, uid in ipairs(activeUsers) do
    local hash = redis.call('HMGET', 'active:session:' .. uid, 'startAt', 'department')
    local startAt = hash[1]
    local dep = hash[2]
    if startAt and dep then
        local delta = now - tonumber(startAt)
        if delta > 0 then
            if not activeByDept[dep] then activeByDept[dep] = {} end
            activeByDept[dep][uid] = delta
        end
    end
end

local deptResults = {}

for i = 1, numDepts do
    local deptName = ARGV[2 + i]
    local deptKey = KEYS[1 + i]
    local confirmed = redis.call('ZRANGE', deptKey, 0, -1, 'WITHSCORES')

    local scoreMap = {}
    local userIds = {}
    for j = 1, #confirmed, 2 do
        local uid = confirmed[j]
        local score = tonumber(confirmed[j + 1])
        scoreMap[uid] = score
        table.insert(userIds, uid)
    end

    local activeForDept = activeByDept[deptName]
    if activeForDept then
        for uid, delta in pairs(activeForDept) do
            if scoreMap[uid] then
                scoreMap[uid] = scoreMap[uid] + delta
            else
                scoreMap[uid] = delta
                table.insert(userIds, uid)
            end
        end
    end

    table.sort(userIds, function(a, b) return scoreMap[a] > scoreMap[b] end)

    local sum = 0
    local limit = math.min(30, #userIds)
    for j = 1, limit do
        sum = sum + scoreMap[userIds[j]]
    end

    table.insert(deptResults, {deptName, sum})
end

table.sort(deptResults, function(a, b) return a[2] > b[2] end)

local flat = {}
table.insert(flat, tostring(#deptResults))
for _, r in ipairs(deptResults) do
    table.insert(flat, r[1])
    table.insert(flat, tostring(r[2]))
end

return flat
