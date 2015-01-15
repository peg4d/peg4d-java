class ParsingObject:
    def __init__(self, pos):
        self.oid = 0
        self.start_pos = pos
        self.end_pos = pos
        self.tag = '#empty'
        self.value = None
        self.parent = None
        self.child = None

class ParsingLog:
    def __init__(self):
        self.next = None
        self.index = 0
        self.childNode = None

class ParsingContext:
    def __init__(self, inputs):
        self.inputs = inputs
        self.pos = 0
        self.left = None
        self.logStackSize = 0
        self.logStack = None
        self.unusedLog = None

    def newLog(self):
        if(self.unusedLog == None):
            l = ParsingLog()
            l.next = None
            l.childNode = None
            return l
        l = self.unusedLog
        self.unusedLog = l.next
        l.next = None
        return l

    def unuseLog(self, log):
        log.childNode = None
        log.next = self.unusedLog
        self.unusedLog = log

    def Parsing_markLogStack(self):
        return self.logStackSize

    def lazyLink(self, parent, index, child):
        l = self.newLog()
        l.childNode = child
        child.parent = parent
        l.index = index
        l.next = self.logStack
        self.logStack = l
        self.logStackSize += 1

    def lazyJoin(self, left):
        l = self.newLog()
        l.childNode = left
        l.index = -9
        l.next = self.logStack
        self.logStack = l
        self.logStackSize += 1

    def commitLog(self, mark, newnode):
        first = None
        objectSize = 0
        while(mark < self.logStackSize) :
            cur = self.logStack
            self.logStack = self.logStack.next
            self.logStackSize--
            if(cur.index == -9) : ## lazyCommit
                self.commitLog(mark, cur.childNode)
                self.unuseLog(cur)
                break
            if(cur.childNode.parent == newnode) :
                cur.next = first
                first = cur
                objectSize += 1
            else :
                self.unuseLog(cur)
        if(objectSize > 0) :
            newnode.child = [None] * objectSize
            newnode.child_size = objectSize
            i = 0
            while(i < objectSize) :
                cur = first
                first = first.next
                if(cur.index == -1) :
                    cur.index = i
                newnode.child[cur.index] = cur.childNode
                self.unuseLog(cur)
                i += 1
            i = 0
            while(i < objectSize) :
                if(newnode.child[i] == None) :
                    newnode.child[i] = ParsingObject(0)
                i += 1

    def abortLog(self, mark):
        while(mark < self.logStackSize) :
            l = self.logStack
            self.logStack = self.logStack.next
            self.logStackSize--
            self.unusedLog(l)

