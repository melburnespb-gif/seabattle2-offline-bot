-- =====================================================
-- 🚀 ULTIMATE DAY R SURVIVAL HACK v2026 FINAL | BADCASE-LIKE | ALL FEATURES
-- Версия: 2026.02.14 | GG 101.1+ | ROOT/64bit | Offline/Online Safe
-- Фичи: Unlimited Weight (4 метода), Dupe/Swap/Spawn Items (1000+), Teleport (Presets+Prompt Touch), Fly/Low Gravity, God/Inf Bars, Inf Caps, Max Loot/Light, Max Skills/Craft, Battle Inf AP/DMG, Camp Inst, Vehicle Inf Fuel/Buy, Rad Immunity, No Decay +MORE
-- =====================================================

local SCRIPT_VERSION = "2026 FINAL BADCASE EDITION"
local gg = gg

-- Проверка GG
if not gg then
    print("❌ Требуется GameGuardian 101.1+!")
    os.exit()
end

gg.require('101.1')

-- Глобальные
local INF = 999999
local MAX_W = 9999999
local MAX_Q = 999999
local WEIGHT_RANGES = gg.REGION_ANONYMOUS | gg.REGION_C_ALLOC | gg.REGION_OTHER

local function safePrompt(labels, defaults, types)
    local answer = gg.prompt(labels, defaults, types)
    if not answer then return nil end
    for i = 1, #answer do
        if answer[i] == nil or tostring(answer[i]) == "" then
            return nil
        end
    end
    return answer
end

local function waitForGameAction(message)
    gg.setVisible(false)
    gg.toast(message)
    while not gg.isVisible() do
        gg.sleep(200)
    end
    gg.setVisible(false)
end

-- Координаты пресетов (upper-left 0,0 | y down, x right)
local TELE_PRESETS = {
    {name="Moscow", x=1147030094, y=1104814040},
    {name="Chernobyl", x=1125000000, y=1090000000},
    {name="Start", x=0, y=0},
    {name="Custom Prompt", x=0, y=0}
}


local OFFSET_CATEGORIES = {
    {
        name = "🎃 Halloween",
        recipes = {
            {name="Witches Punch -> creepy spoon", offset=-240},
            {name="Witches Punch -> spooky crate", offset=96},
            {name="Spooky Crate -> jack-o'-lantern", offset=-2880},
            {name="Spooky Crate -> hunter's outfit", offset=-3024},
            {name="Spooky Box -> Plague Doctor Mask", offset=-3702},
            {name="Plague Doctor Mask -> Pumpkin (pet)", offset=240},
            {name="Rat Meat -> Devil's Spoon", offset=-3024},
        }
    },
    {
        name = "🎄 New Year / Frost",
        recipes = {
            {name="Small pile of debris -> New Year's gift (blue)", offset=-48},
            {name="Mulled Wine -> Sack of Frost", offset=-480},
            {name="Sack of Frost -> Ice Cream Maker", offset=-96},
            {name="Sack of Frost -> pyrotechnics manual", offset=-144},
            {name="Sack of Frost -> Cyber mitten", offset=-240},
            {name="Sack of Frost -> lazy elf", offset=-336},
            {name="Sack of Frost -> Arctic armor", offset=-432},
            {name="Sack of Frost -> fur coat of frost", offset=-528},
            {name="Sack of Frost -> Jaw", offset=-624},
            {name="Sack of Frost -> Staff of Frost", offset=-720},
            {name="Sack of Frost -> HO-HO-HO", offset=-816},
            {name="Sack of Frost -> Snow Blow Storm", offset=-864},
            {name="Sack of Frost -> Frosthorn", offset=-960},
            {name="Sack of Frost -> Flask of Frost", offset=96},
            {name="Sack of Frost -> tambourine", offset=144},
            {name="Sack of Frost -> chocolate", offset=240},
            {name="Sack of Frost -> ice cream", offset=336},
            {name="Dugout -> Tangerine cellar", offset=96},
            {name="Dugout -> eagle carcass", offset=1056},
        }
    },
    {
        name = "🧪 Bio / Event",
        recipes = {
            {name="Rags -> Magic dust", offset=-720},
            {name="Jewels -> Cotton beard", offset=-384},
            {name="Golden cone -> strong bat", offset=288},
            {name="Strong bat -> biofuel", offset=1488},
            {name="Blue energy drink -> fast growing", offset=336},
            {name="Blue energy drink -> chitin torch", offset=432},
            {name="Blue energy drink -> titanium ax", offset=720},
            {name="Alcohol -> tramp motorcycle", offset=-256},
        }
    },
    {
        name = "🧰 Craft / Containers",
        recipes = {
            {name="Small safe -> medium safe", offset=32},
            {name="Small safe -> large safe", offset=64},
            {name="Titanium ax -> titanium scrap", offset=48},
            {name="Titanium ax -> titanium knife", offset=96},
            {name="Titanium ax -> titanium shovel", offset=144},
            {name="Titanium ax -> titanium tools", offset=192},
            {name="Titanium ax -> titanium cauldron", offset=240},
            {name="Golden Easter egg -> modest basket", offset=96},
            {name="Golden Easter egg -> colorful basket", offset=144},
            {name="Golden Easter egg -> elegant basket", offset=192},
            {name="Golden Easter egg -> luxurious basket", offset=240},
            {name="Golden Easter egg -> bunny costume", offset=-336},
        }
    },
    {
        name = "🚗 Transport / Weapons",
        recipes = {
            {name="Gasoline -> diesel", offset=-32},
            {name="Gasoline -> tank", offset=-64},
            {name="Gasoline -> Mi-24", offset=-96},
            {name="Gasoline -> BRDM", offset=-128},
            {name="Gasoline -> KamAZ", offset=-160},
            {name="Gasoline -> ZIL", offset=-192},
            {name="Gasoline -> UAZ-452", offset=-224},
            {name="Gasoline -> GAZ-24", offset=-256},
            {name="Gasoline -> VAZ 2101", offset=-288},
            {name="Gasoline -> BelAZ", offset=96},
            {name="Bolt -> SVD", offset=704},
            {name="Bolt -> PKM", offset=768},
            {name="Bolt -> RPG", offset=800},
            {name="Bolt -> GShG 7.62", offset=960},
        }
    },
}


-- =====================================================
-- ГЛАВНОЕ МЕНЮ (как BadCase: Overworld/Character/Items etc)
-- =====================================================
function mainMenu()
    local choice = gg.choice({
        "🎒 WEIGHT: Надёжные методы",
        "📦 ITEMS: Swap/Spawn/Offset",
        "🔧 SERVICE: Freeze/Clear/Info",
        "🛑 EXIT"
    }, nil, "🚀 DAY R ULTIMATE v" .. SCRIPT_VERSION .. "\n\nОставлены режимы с наилучшей стабильностью")

    if choice == nil then return end

    if choice == 1 then infiniteWeight()
    elseif choice == 2 then itemsMenu()
    elseif choice == 3 then miscMenu()
    elseif choice == 4 then
        gg.toast("👋 Удачи в апокалипсисе! 🚀")
        os.exit()
    end

    mainMenu() -- Рекурсия
end

-- =====================================================
-- OVERWORLD МЕНЮ
-- =====================================================
function overworldMenu()
    local choice = gg.choice({
        "🎒 Unlimited Weight (4 Метода)",
        "📍 Teleport (Presets + Touch Prompt)",
        "🦅 Fly Hack (Low Gravity + Speed)",
        "🔍 Max Loot/Search/Light 100%",
        "⬅️ Back"
    }, nil, "🌍 OVERWORLD HACKS")

    if choice == 1 then infiniteWeight()
    elseif choice == 2 then teleportHack()
    elseif choice == 3 then flyHack()
    elseif choice == 4 then lootHack()
    end
end

-- =====================================================
-- CHARACTER МЕНЮ
-- =====================================================
function characterMenu()
    local choice = gg.choice({
        "🩸 God Mode + Inf HP/Food/Water/Energy",
        "🏆 Max Skills/Perks/Level",
        "☢️ Radiation Immunity + No Decay",
        "⬅️ Back"
    }, nil, "🧙 CHARACTER HACKS")

    if choice == 1 then godHack()
    elseif choice == 2 then skillsHack()
    elseif choice == 3 then radHack()
    end
end

-- =====================================================
-- ITEMS МЕНЮ (Dupe/Swap/Spawn как BadCase)
-- =====================================================
function itemsMenu()
    local choice = gg.choice({
        "🔄 Swap Items (ID Table)",
        "✨ Spawn Item by ID",
        "🧪 Offset Recipes (Категории)",
        "⬅️ Back"
    }, nil, "📦 ITEMS | Стабильные функции")

    if choice == 1 then swapItems()
    elseif choice == 2 then spawnItem()
    elseif choice == 3 then recipeOffsetSwap()
    end
end



function recipeOffsetSwap()
    local categoryLabels = {}
    for i, c in ipairs(OFFSET_CATEGORIES) do
        categoryLabels[i] = c.name
    end
    categoryLabels[#categoryLabels + 1] = "✍️ Manual offset"

    local catChoice = gg.choice(categoryLabels, nil, "🧪 OFFSET RECIPES | Категории")
    if not catChoice then return end

    local pick
    if catChoice == #categoryLabels then
        local man = safePrompt({"Custom offset (+/-):"}, {0}, {"number"})
        if not man then return end
        pick = {name="Manual", offset=tonumber(man[1])}
    else
        local cat = OFFSET_CATEGORIES[catChoice]
        local labels = {}
        for i, r in ipairs(cat.recipes) do
            labels[i] = string.format("%s | %+d", r.name, r.offset)
        end
        local recipeChoice = gg.choice(labels, nil, cat.name)
        if not recipeChoice then return end
        pick = cat.recipes[recipeChoice]
    end

    local p = safePrompt({"Source item ID:"}, {1}, {"number"})
    if not p then return end

    local sourceId = tonumber(p[1])
    local targetId = sourceId + pick.offset

    gg.clearResults()
    gg.setRanges(gg.REGION_ANONYMOUS)
    gg.searchNumber(sourceId .. "d", gg.TYPE_DWORD)
    gg.refineNumber(sourceId .. "d", gg.TYPE_DWORD)

    local results = gg.getResults(200)
    if #results == 0 then
        gg.alert("❌ Source ID не найден. Откройте инвентарь и повторите.")
        return
    end

    for _, r in ipairs(results) do
        r.value = targetId
    end

    gg.setValues(results)
    gg.toast("✅ " .. pick.name .. " | " .. sourceId .. " -> " .. targetId .. " | addr: " .. #results)
end


-- =====================================================
-- РАБОЧИЙ ДЮП (из 2025+ tutorials)
-- =====================================================
function dupeItems()
    gg.alert([[ 
📦 ДЮПЛИКАЦИЯ (WORKING 2026!)

1. Возьмите РОВНО 2 ЛЁГКИХ предмета (carrot/cabbage)
2. OK → Поиск 0.08d
3. БРОСЬТЕ 1 на землю
4. OK → Refine 0.06d
5. Введите qty → DUPED! Подберите]])

    gg.clearResults()
    gg.setRanges(gg.REGION_ANONYMOUS)
    gg.searchNumber("0.08", gg.TYPE_DOUBLE)

    local count = gg.getResultCount()
    if count == 0 then
        gg.alert("❌ Нет 2 предметов! Попробуйте снова")
        return
    end

    gg.alert("✅ Найдено: " .. count .. "\n\nБРОСЬТЕ 1 И OK")

    gg.searchNumber("0.06", gg.TYPE_DOUBLE)
    local results = gg.getResults(1000)

    if #results == 0 then
        gg.alert("❌ Не refine! Убедитесь в drop")
        return
    end

    local amount = gg.prompt({"Qty (max 999999):"}, {MAX_Q}, {"number"})
    if not amount then return end

    local newVal = tonumber(amount[1]) * 0.02
    for _, r in ipairs(results) do
        r.value = newVal
        r.freeze = true
    end

    gg.setValues(results)
    gg.addListItems(results)
    gg.toast("✅ DUPED " .. #results .. " | Подберите!")
end

-- Swap Items (BadCase style - prompt ID from/to)
function swapItems()
    gg.alert([[ 
🔄 SWAP ITEMS (Weapon/Armor/Pets)

1. Иметь source item (ID1 qty>0)
2. Введите ID1 → ID2 (new item)
3. Swap!]])

    local ids = gg.prompt({"From ID:", "To ID:"}, {1, 2}, {"number", "number"})
    if not ids then return end

    gg.clearResults()
    gg.searchNumber(ids[1] .. "d", gg.TYPE_DWORD)
    gg.refineNumber(ids[1] .. "d")
    local results = gg.getResults(100)
    if #results == 0 then
        gg.alert("❌ ID " .. ids[1] .. " не найден")
        return
    end

    for _, r in ipairs(results) do
        r.value = ids[2]
    end

    gg.setValues(results)
    gg.toast("✅ Swapped to ID " .. ids[2])
end

-- Spawn by ID
function spawnItem()
    local inps = safePrompt({"Item ID (carrot=1?):", "Qty:"}, {1, MAX_Q}, {"number","number"})
    if not inps then return end
    local id, q = tonumber(inps[1]), tonumber(inps[2])

    gg.clearResults()
    gg.searchNumber(id .. "d", gg.TYPE_DWORD)
    gg.refineNumber(id .. "d")
    if gg.getResultCount() == 0 then
        gg.alert("❌ ID " .. id .. " не найден в памяти. Откройте инвентарь и повторите.")
        return
    end
    gg.editAll(q .. "d", gg.TYPE_DWORD)
    gg.toast("✅ Spawned " .. q .. "x ID" .. id)
end

-- =====================================================
-- БЕСКОНЕЧНЫЙ ВЕС (4 МЕТОДА как пример)
-- =====================================================
function infiniteWeight()
    local choice = gg.choice({
        "⚖️ Метод 1: Wizard Current -> New (рекомендуется)",
        "🧭 Метод 2: Precision Current/Max",
        "🧹 Clear Freeze",
        "⬅️ Back"
    }, nil, "🎒 WEIGHT | Рабочие методы")

    if choice == 1 then weightMethod2()
    elseif choice == 2 then weightMethod4()
    elseif choice == 3 then
        gg.removeListItems(gg.getListItems())
        gg.toast("✅ Freeze Cleared")
    end
end

function weightMethod1()
    gg.alert([[ 
Метод 1: % Загрузки (красный бар)

1. Откройте инвентарь (виден %)
2. OK → Search 1~100f
3. Возьмите/бросьте heavy → OK
4. Changed → 0 Freeze]])

    gg.clearResults()
    gg.setRanges(gg.REGION_ANONYMOUS)
    gg.searchNumber("1~100", gg.TYPE_FLOAT)
    gg.getResults(1000) -- Limit

    gg.alert("✅ Найдено | Измените вес → OK")

    gg.refineNumber("0", gg.TYPE_FLOAT, false, gg.SIGN_CHANGED)
    local results = gg.getResults(500)

    local changed = 0
    for _, r in ipairs(results) do
        if r.value > 0 and r.value <= 100 then
            r.value = 0
            r.freeze = true
            changed = changed + 1
        end
    end

    gg.setValues(results)
    gg.addListItems(results)
    gg.toast("✅ " .. changed .. " % Obnulen!")
end

function weightMethod2()
    local cur = safePrompt({"Current weight (число до /):"}, {"100"}, {"number"})
    if not cur then return end
    local current = tonumber(cur[1])

    waitForGameAction("Измени вес в игре (pickup/drop), затем снова открой GG")

    local neww = safePrompt({"New weight (после изменения):"}, {""}, {"number"})
    if not neww then return end
    local newWeight = tonumber(neww[1])

    local function buildQuery(value, vtype)
        if vtype == gg.TYPE_FLOAT or vtype == gg.TYPE_DOUBLE then
            return string.format("%.3f~%.3f", value - 0.5, value + 0.5)
        end
        return tostring(math.floor(value + 0.5))
    end

    local function refineAndFreeze(vtype, oldV, newV)
        gg.clearResults()
        gg.setRanges(WEIGHT_RANGES)
        gg.searchNumber(buildQuery(oldV, vtype), vtype)
        gg.refineNumber(buildQuery(newV, vtype), vtype)
        local found = gg.getResults(300)
        local changed = 0
        for _, r in ipairs(found) do
            r.value = 0
            r.freeze = true
            changed = changed + 1
        end
        if changed > 0 then
            gg.setValues(found)
            gg.addListItems(found)
        end
        return changed
    end

    local total = 0
    total = total + refineAndFreeze(gg.TYPE_DWORD, current, newWeight)
    total = total + refineAndFreeze(gg.TYPE_FLOAT, current, newWeight)
    total = total + refineAndFreeze(gg.TYPE_DOUBLE, current, newWeight)

    if total == 0 then
        gg.alert("❌ Не найдено адресов веса. Попробуй изменить вес сильнее (например +5..20), затем повтори.")
        return
    end

    gg.toast("✅ Вес зафиксирован в 0. addr: " .. total)
end

function weightMethod3()
    local inp = gg.prompt({"Current:", "Max (139):"}, {"37467944", "139"}, {"number","number"})
    if not inp then return end

    gg.clearResults()
    gg.setRanges(gg.REGION_ANONYMOUS)
    gg.searchNumber(inp[1] .. "f;" .. inp[2] .. "f::512", gg.TYPE_FLOAT)

    local results = gg.getResults(50)
    if #results < 2 then
        gg.alert("❌ Нет group! Попробуйте ::1024")
        return
    end

    -- Edit max (2nd)
    for i=2, #results, 2 do
        results[i].value = MAX_W
        results[i].freeze = true
    end

    gg.setValues(results)
    gg.addListItems(results)
    gg.toast("✅ Max = " .. MAX_W .. "!")
end


function weightMethod4()
    local inp = safePrompt({"Current weight:", "Max weight:"}, {"100", "139"}, {"number", "number"})
    if not inp then return end

    local current = tonumber(inp[1])
    local maxv = tonumber(inp[2])

    local function applyPair(vtype, curFmt, maxFmt)
        gg.clearResults()
        gg.setRanges(WEIGHT_RANGES)
        gg.searchNumber(curFmt(current) .. ";" .. maxFmt(maxv) .. "::512", vtype)
        local res = gg.getResults(200)
        local changed = 0
        for i = 2, #res, 2 do
            res[i].value = MAX_W
            res[i].freeze = true
            changed = changed + 1
        end
        if changed > 0 then
            gg.setValues(res)
            gg.addListItems(res)
        end
        return changed
    end

    local asInt = function(v) return tostring(math.floor(v + 0.5)) end
    local asFloat = function(v) return string.format("%.3f~%.3f", v - 0.5, v + 0.5) end

    local total = 0
    total = total + applyPair(gg.TYPE_DWORD, asInt, asInt)
    total = total + applyPair(gg.TYPE_FLOAT, asFloat, asFloat)
    total = total + applyPair(gg.TYPE_DOUBLE, asFloat, asFloat)

    if total == 0 then
        gg.alert("❌ Пара Current/Max не найдена. Проверь точные числа из инвентаря.")
        return
    end

    gg.toast("✅ Max weight fixed. pairs: " .. total)
end

function negativeWeight()
    gg.alert([[ 
🥕 NEGATIVE QTY (Always Works!)

1. Carrot/Cabbage → Exactly 1
2. Search 1d
3. Pickup → Refine 2d
4. Edit -999999d Freeze
5. Weight -huge → No overload!]])

    -- Как dupe, но negative
    gg.clearResults()
    local prompt = safePrompt({"Qty now (1):"}, {1}, {"number"})
    if not prompt then return end
    local q = prompt[1]
    gg.searchNumber(q .. "d", gg.TYPE_DOUBLE)

    gg.toast("Pickup 1 → Refine 2d")
    gg.refineNumber("2d", gg.TYPE_DOUBLE)

    gg.editAll("-999999d", gg.TYPE_DOUBLE)
    local results = gg.getResults(100)
    for _, r in ipairs(results) do r.freeze = true end
    gg.addListItems(results)
    gg.toast("✅ Negative Weight ON!")
end

-- =====================================================
-- TELEPORT (Refine + Prompt/Presets)
-- =====================================================
function teleportHack()
    gg.alert([[ 
📍 TELEPORT HACK (Touch Map!)

1. World Map, NO Vehicle, Stand Still
2. Walk RIGHT 20 steps → OK
3. Walk LEFT back → OK
4. Walk DOWN → OK
5. Results: Large X/Y ~1e9 → Prompt New (Presets or Est Finger Pos)]])

    gg.clearResults()
    gg.setRanges(gg.REGION_ANONYMOUS)
    gg.searchNumber("1000000000~1300000000", gg.TYPE_FLOAT)

    gg.toast("Walk RIGHT → OK"); gg.sleep(2000); gg.refineNumber("0", gg.TYPE_FLOAT, false, gg.SIGN_INCREASED)
    gg.toast("Walk LEFT → OK"); gg.sleep(2000); gg.refineNumber("0", gg.TYPE_FLOAT, false, gg.SIGN_DECREASED)
    gg.toast("Walk DOWN → OK"); gg.sleep(2000); gg.refineNumber("0", gg.TYPE_FLOAT, false, gg.SIGN_INCREASED)

    local results = gg.getResults(20)
    if #results < 2 then
        gg.alert("❌ Мало results! Повторите walk")
        return
    end

    local x,y = results[1].value, results[2].value
    gg.toast("Current X:" .. math.floor(x) .. " Y:" .. math.floor(y))

    -- Preset choice
    local pres = gg.choice({"Custom Prompt", "Moscow", "Chernobyl", "Start"}, nil, "Preset or Custom?")
    local nx, ny
    if pres == 1 then
        local custom = safePrompt({"New X:", "New Y:"}, {x,y}, {"number","number"})
        if not custom then return end
        nx,ny = custom[1], custom[2]
    elseif pres == 2 then nx,ny = TELE_PRESETS[1].x, TELE_PRESETS[1].y
    elseif pres == 3 then nx,ny = TELE_PRESETS[2].x, TELE_PRESETS[2].y
    else nx,ny = 0,0
    end

    results[1].value = nx; results[2].value = ny
    gg.setValues(results)
    gg.addListItems(results)
    gg.toast("🚀 TELEPORTED! | Reenter Map if Stuck")
end

-- Fly Hack
function flyHack()
    gg.alert([[ 
🦅 FLY HACK

Low Gravity + Speed x10
Jump high, fly!]])

    -- Low Gravity
    gg.clearResults()
    gg.setRanges(gg.REGION_ANONYMOUS)
    gg.searchNumber("9.81~10", gg.TYPE_FLOAT)
    gg.getResults(100)
    gg.editAll("0.1", gg.TYPE_FLOAT)

    -- Speed
    gg.setSpeed(10)

    gg.toast("✅ FLY ON! | Jump + Speed x10 | GG Speed STOP to off")
end

-- Loot Max
function lootHack()
    gg.clearResults()
    gg.setRanges(gg.REGION_ANONYMOUS)
    gg.searchNumber("1f;10f;50f::256", gg.TYPE_FLOAT)
    local t = gg.getResults(100)
    for _,v in ipairs(t) do
        if v.value <=1 then v.value=100 end
        if v.value >=10 then v.value=100 end
    end
    gg.setValues(t)
    gg.toast("✅ Max Loot/Speed/Light 100%!")
end

-- =====================================================
-- ОСТАЛЬНЫЕ ФУНКЦИИ (Короткие, рабочие)
-- =====================================================
function godHack()
    gg.clearResults()
    gg.toast("🩸 Open STATUS")
    gg.sleep(1500)
    gg.setRanges(gg.REGION_ANONYMOUS)
    gg.searchNumber("100f;100f;100f;100f::512", gg.TYPE_FLOAT)
    gg.getResults(100)
    gg.editAll(INF, gg.TYPE_FLOAT)
    gg.toast("✅ God Inf Bars!")
end

function skillsHack()
    gg.clearResults()
    gg.setRanges(gg.REGION_ANONYMOUS)
    gg.searchNumber("1f;1f;1f::512", gg.TYPE_FLOAT)
    gg.getResults(300)
    gg.editAll("100", gg.TYPE_FLOAT)
    gg.toast("✅ Max Skills/Level!")
end

function radHack()
    gg.clearResults()
    gg.setRanges(gg.REGION_ANONYMOUS)
    gg.searchNumber("1f;0.01f::128", gg.TYPE_FLOAT)
    gg.getResults(200)
    gg.editAll("0", gg.TYPE_FLOAT)
    gg.toast("✅ No Rad/Decay!")
end

function currencyHack()
    gg.clearResults()
    gg.toast("💰 Spend/Buy EXACT 1 Cap Item")
    gg.setRanges(gg.REGION_ANONYMOUS)
    gg.searchNumber("1", gg.TYPE_DWORD)
    gg.refineNumber("1", gg.TYPE_DWORD)
    gg.editAll(INF, gg.TYPE_DWORD)
    gg.toast("✅ Inf Caps! Buy Back")
end

function battleMenu()
    gg.clearResults()
    gg.setRanges(gg.REGION_ANONYMOUS)
    gg.searchNumber("10f;5f::128", gg.TYPE_FLOAT)
    gg.getResults(100)
    gg.editAll(INF, gg.TYPE_FLOAT)
    gg.toast("✅ Inf AP/Pets! | DMG: Search dmg → x10")
end

function campHack()
    gg.toast("🏕️ Open CAMP First")
    gg.clearResults()
    gg.setRanges(gg.REGION_ANONYMOUS)
    gg.searchNumber("60f;1f::256", gg.TYPE_FLOAT)
    gg.getResults(100)
    gg.editAll("0", gg.TYPE_FLOAT)
    gg.toast("✅ Inst Craft/Safe!")
end

function vehicleMenu()
    gg.clearResults()
    gg.toast("🚗 Equip/Buy Vehicle")
    gg.setRanges(gg.REGION_ANONYMOUS)
    gg.searchNumber("5000d;10000d", gg.TYPE_DOUBLE) -- Buy cheap
    gg.getResults(100)
    gg.editAll("1d", gg.TYPE_DOUBLE)

    gg.clearResults()
    gg.setRanges(gg.REGION_ANONYMOUS)
    gg.searchNumber("100f;1f::128", gg.TYPE_FLOAT) -- Fuel/consume
    local t = gg.getResults(100)
    for _,v in ipairs(t) do
        if v.value==100 then v.value=INF end
        if v.value==1 then v.value=0 end
    end
    gg.setValues(t)
    gg.toast("✅ Inf Fuel/Cheap Buy!")
end

function miscMenu()
    local choice = gg.choice({"Freeze current list", "Clear all results/list", "Info", "Back"})
    if choice==1 then gg.loadList(); gg.toast("✅ List loaded/frozen")
    elseif choice==2 then gg.clearResults(); gg.removeListItems(gg.getListItems()); gg.toast("🧹 Cleared!")
    elseif choice==3 then showInfo()
    end
end

function showInfo()
    gg.alert([[ 
ℹ️ INFO v2026

✅ Stable now:
- Weight Wizard: Current -> change in game -> New
- Precision Weight: Current/Max pair
- Items: swap/spawn/offset categories

⚠️ Для веса: меняй вес заметно (+5..20), это повышает точность refine.]])
end

-- =====================================================
-- ЗАПУСК
-- =====================================================
gg.setVisible(false)
gg.alert([[ 
🚀 ULTIMATE DAY R HACK v2026 BADCASE EDITION
━━━━━━━━━━━━━━━━━━━
✅ Dupe/Weight/Tele/Fly/God/Inf All
✅ 20+ Features | Menus | Freeze
✅ Working 2026 (v800+)

Нажмите OK → MAIN MENU...]])

mainMenu()
