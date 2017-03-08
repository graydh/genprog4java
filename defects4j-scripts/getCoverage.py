#/bin/python

import argparse
import os
import xml.etree.ElementTree
import subprocess

d4jHome = os.environ['D4J_HOME']
defects4jCommand = d4jHome + "/framework/bin/defects4j"

class BugInfo(object):
	def __init__(self, project, bugNum, wd, testDir):
		self.project = project
		self.bugNum = bugNum
		self.buggyFolder = wd + "/" + project.lower() + str(bugNum) + "Buggy"
		self.fixedFolder = wd + "/" + project.lower() + str(bugNum) + "Fixed"
		self.testDir = testDir
		self.ensureVersionAreCheckedOut()

	def getProject(self):
		return str(self.project)

	def getBugNum(self):
		return str(self.bugNum)

	def getFixPath(self):
		return str(os.path.join(d4jHome, self.fixedFolder))

	def getBugPath(self):
		return str(os.path.join(d4jHome, self.buggyFolder))

	def getTestDir(self):
		return str(os.path.join(d4jHome, self.testDir))
	
	def ensureVersionAreCheckedOut(self):
		if(not os.path.exists(self.getBugPath())):
			self.checkout(self.getBugPath(), "b")
		if(not os.path.exists(self.getFixPath())):
			self.checkout(self.getFixPath(), "f")

	def checkout(self, folderToCheckout, vers):
		cmd = defects4jCommand + " checkout -p " + self.project + " -v " + self.bugNum + vers + " -w " +folderToCheckout
		p = subprocess.call(cmd, shell=True) #, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
		
def computeCoverage(listOfChangedLines, coverageFile):
#	if(not (args.coverage is None)):
	e = xml.etree.ElementTree.parse(coverageFile).getroot()
	#print e
	lines = e.findall(".//line")
	#print listOfChangedLines
	realLines = []
	lineNumbersCoveredAlready = []
	for line in lines:
		if(line.attrib['number'] in listOfChangedLines):
			if(not (line.attrib['number'] in lineNumbersCoveredAlready)):
				realLines.append(line)
				lineNumbersCoveredAlready.append(line.attrib['number'])

	linesCovered=0
	for realLine in realLines:
		# check if covered
		#print realLine.attrib['hits']
		if(int(realLine.attrib['hits']) != 0):
			linesCovered += 1

		methodsChanged = printMethodCorrespondingToLine(realLine.attrib['number'], e)

	linesChanged=len(realLines)
	percentageLinesCovered=linesCovered*100/linesChanged
	print "Lines modified: " + str(linesChanged) 
	print "Percentage of modified lines covered: " + str(percentageLinesCovered) + "%"
	print "Methods changed and corresponding Line/Branch coverage: " + str(methodsChanged)

def printMethodCorrespondingToLine(lineNum, tree):
	methodsChanged=[]
	for method in tree.findall(".//method"):
		#print method.attrib['name']
		lines = method.find("lines")
		for line in lines:
			#print line.attrib['number']
			#print lineNum
			if(line.attrib['number'] == lineNum):
				methodLineCov= float(method.attrib['line-rate'])*100
				methodBranchCov=float(method.attrib['branch-rate'])*100
				methodsChanged.append(method.attrib['name']+": Line:" + str(methodLineCov) + "%" + " Branch:" + str(methodBranchCov) + "%" )
	return methodsChanged

def generateCovXML(bug, tool, seed):
	if(tool.lower() == "evosuite"):
		testSuiteName="evosuite-branch"
	elif(tool.lower() == "randoop"):
		testSuiteName="randoop"
	suitePath =  os.path.join(bug.getTestDir(), bug.getProject()+"-"+bug.getBugNum()+"f-"+testSuiteName+"."+str(seed)+".tar.bz2")
	cmd = defects4jCommand + " coverage -w " + bug.getFixPath() + " -s " + str(suitePath)
	subprocess.call(cmd, shell=True) # this doesn't save the log or do any kind of error checking (yet!)

def getEditedFiles(bug):
	cmd = defects4jCommand + " export -p classes.modified"
	p = subprocess.Popen(cmd, shell=True, cwd=bug.getBugPath(), stdout=subprocess.PIPE, stderr=subprocess.PIPE)
	return [ line.strip().replace(".", "/") + ".java" for line in p.stdout ]

# assume that file1, file2 are java files
def getADiff(buggyPath, fixedPath, pathToFile, bug):
	cmd = defects4jCommand + " export -p dir.src.classes"
	p = subprocess.Popen(cmd, shell=True, cwd=bug.getBugPath(), stdout=subprocess.PIPE, stderr=subprocess.PIPE)
	for line in p.stdout:
		pathToSource=line
	#print pathToSource

        cmd = "diff --unchanged-line-format=\"\"  --old-line-format=\"%dn \" --new-line-format=\"%dn \" " + buggyPath+"/"+pathToSource+"/"+pathToFile +" " + fixedPath+"/"+pathToSource+"/"+pathToFile
#        print cmd
        p = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE)
        for line in p.stdout:
#			print "Line: " + line
			diffLines = line
#	print "Diff lines: "+diffLines
	return diffLines.split()
	

def getOptions():
	parser = argparse.ArgumentParser(description="This script checks if a test suite is covering the human changes")
	parser.add_argument("wd", help="working directory to check out project versions")
	parser.add_argument("testDir", help="the path where the test suite is located, starting from the the D4J_HOME folder (Example: generatedTestSuites)")
	parser.add_argument("--project", help="the project in upper case (ex: Lang, Chart, Closure, Math, Time)")
	parser.add_argument("--bug", help="the bug number (ex: 1,2,3,4,...)")
	parser.add_argument("--many", help="file listing bugs to process: project,bugNum (one per line). Lines starting with # are skipped")
	parser.add_argument("--tool", help="the generation tool (Randoop or Evosuite)", default="Evosuite")
	parser.add_argument("--seed", help="the seed the test suite was created with", default="1")
	parser.add_argument("--coverage", help="a coverage file")
	return parser.parse_args()

#args.many is assumed to be not None
def getAllBugs(bugs,args):
	if(not os.path.isfile(args.many)):
		# MAU TODO: ADD GRACEFUL ERROR HANDLING/MESSAGE HERE
		return
	else:
		with open(args.many) as f:
			pairs = [x.strip().split(',') for x in f.readlines() if x[0] != '#']
			for pair in pairs:
				bug = BugInfo(pair[0], int(pair[1]), args.wd, args.testDir)
				bugs.append(bug)
def main():
	args=getOptions()
	# TODO: insert error handling/sanity checking to be sure the appropriate environment variables are set and abort with an error/usage message if not
	# TODO: line wrap this file at 80 characters or so
	bugs = []
	if(args.many == None):
		bugs.append(BugInfo(args.project, args.bug, args.wd, args.testDir))
	else:
		getAllBugs(bugs, args)
	for bug in bugs:
		if((args.coverage == None) and (not os.path.exists(bug.getFixPath()+"/coverage.xml"))):
			generateCovXML(bug,args.tool, args.seed)
		for f in getEditedFiles(bug):
			listOfChangedLines = getADiff(bug.getBugPath(),bug.getFixPath(), f, bug)
			computeCoverage(listOfChangedLines, bug.getFixPath()+"/coverage.xml")

main()
