package com.tridium.template.application;

import java.util.Stack;

public class ProgressTracker {
   private int currentProgress = 0;
   private Stack<Integer> constraintStarts = new Stack<>();
   private Stack<Integer> constraintEnds = new Stack<>();
   private Stack<Integer> cycleCount = new Stack<>();
   private Stack<Integer> cycleNumber = new Stack<>();

   public void message(String lexKey, String... args) {
   }

   public void heartbeat() {
   }

   public void endFailed(Throwable exception) {
   }

   public int get() {
      return this.currentProgress;
   }

   public void step(int percent) {
      this.progress(this.overallPercent(percent));
   }

   public void constrain(int percent) {
      this.constrainOverall(this.overallPercent(percent));
   }

   public void fulfill() {
      this.progress(this.popConstraints());
   }

   public void divide(int cycleCount) {
      int cstart = this.currentProgress;
      int cend = this.constraintEnd();
      this.constrainOverall(cend);
      this.cycleCount.push(cycleCount);
      this.cycleNumber.push(0);
      if (cycleCount > 0) {
         this.constrainOverall(cstart + (cend - cstart) / cycleCount);
      } else {
         this.progress(cend);
      }
   }

   public void conquer() {
      if (this.cycleNumber.peek() < this.cycleCount.peek()) {
         this.fulfill();
      }

      this.cycleCount.pop();
      this.cycleNumber.pop();
      this.fulfill();
   }

   public void cycle() {
      if (this.cycleNumber.peek() < this.cycleCount.peek()) {
         this.fulfill();
         this.cycleNumber.push(this.cycleNumber.pop() + 1);
         if (this.cycleNumber.peek() < this.cycleCount.peek()) {
            int cstart = this.constraintStart();
            int cend = this.constraintEnd();
            this.constrainOverall(cstart + (cend - cstart) * (this.cycleNumber.peek() + 1) / this.cycleCount.peek());
         }
      }
   }

   private void constrainOverall(int overallPercent) {
      this.pushConstraints(this.currentProgress, overallPercent);
   }

   private int constraintStart() {
      return this.constraintStarts.empty() ? 0 : this.constraintStarts.peek();
   }

   private int constraintEnd() {
      return this.constraintEnds.empty() ? 100 : this.constraintEnds.peek();
   }

   private int overallPercent(int relativePercent) {
      int cstart = this.constraintStart();
      int cend = this.constraintEnd();
      return cstart + (cend - cstart) * relativePercent / 100;
   }

   private void pushConstraints(int start, int end) {
      this.constraintStarts.push(start);
      this.constraintEnds.push(end);
   }

   private int popConstraints() {
      if (this.constraintEnds.empty()) {
         return 100;
      } else {
         this.constraintStarts.pop();
         return this.constraintEnds.pop();
      }
   }

   protected void progress(int percent) {
      this.currentProgress = percent;
   }
}
