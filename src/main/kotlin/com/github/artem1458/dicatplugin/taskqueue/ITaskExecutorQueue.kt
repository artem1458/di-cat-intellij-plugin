package com.github.artem1458.dicatplugin.taskqueue

interface ITaskExecutorQueue<T : ITaskExecutorQueue.ITask> {
  fun start()
  fun stop()
  fun clear()
  fun add(task: T)
  fun add(tasks: List<T>)

  interface ITask
}
