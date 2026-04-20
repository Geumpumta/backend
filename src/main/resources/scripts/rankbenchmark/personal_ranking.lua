-- Personal ranking: full ZSET scan + active session merge + full sort.
-- KEYS[1] = confirmed ZSET (rank:user:{period}:{date})
-- KEYS[2] = active:users set
-- ARGV[1] = now (epoch ms)
-- ARGV[2] = topK
-- ARGV[3] = my userId (string)
--
-- Return (flat array): totalUsers, topCount, (uid, score) * topCount, myRank, myScore

local confirmedKey = KEYS[1]
local activeUsersKey = KEYS[2]
local now = tonumber(ARGV[1])
local topK = tonumber(ARGV[2])
local myUserId = ARGV[3]

local confirmed = redis.call('ZRANGE', confirmedKey, 0, -1, 'WITHSCORES')
local activeUsers = redis.call('SMEMBERS', activeUsersKey)

local scoreMap = {}
local userIds = {}

for i = 1, #confirmed, 2 do
    local uid = confirmed[i]
    local score = tonumber(confirmed[i + 1])
    scoreMap[uid] = score
    table.insert(userIds, uid)
end

for _, uid in ipairs(activeUsers) do
    local startAt = redis.call('HGET', 'active:session:' .. uid, 'startAt')
    if startAt then
        local delta = now - tonumber(startAt)
        if delta > 0 then
            if scoreMap[uid] then
                scoreMap[uid] = scoreMap[uid] + delta
            else
                scoreMap[uid] = delta
                table.insert(userIds, uid)
            end
        end
    end
end

table.sort(userIds, function(a, b) return scoreMap[a] > scoreMap[b] end)

local totalUsers = #userIds
local limit = math.min(topK, totalUsers)

local result = {}
table.insert(result, tostring(totalUsers))
table.insert(result, tostring(limit))
for i = 1, limit do
    table.insert(result, userIds[i])
    table.insert(result, tostring(scoreMap[userIds[i]]))
end

local myRank = 0
for i = 1, totalUsers do
    if userIds[i] == myUserId then
        myRank = i
        break
    end
end
if myRank == 0 then myRank = totalUsers + 1 end
local myScore = scoreMap[myUserId] or 0

table.insert(result, tostring(myRank))
table.insert(result, tostring(myScore))

return result
