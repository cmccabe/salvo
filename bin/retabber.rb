#!/usr/bin/ruby -w
require 'find'

lines = Array.new

Find.find(".") do |path|
  next unless File.file?(path)
  if (path =~ /.java$/) then
    file = File.new(path, "r")
    while (line = file.gets)
      line.chomp!
      if (line =~ /\t/) then
        found_problems = true
        line.gsub!("\t", '    ')
      end
      while (line =~ / $/)
        found_problems = true
        line.chop!
      end
      #puts '"' + line + '"'
      lines << line
    end
    if (found_problems) then
      file = File.new(path, "w")
      lines.each do |l|
        file.puts l
      end
    end
    lines.clear()
  end
end
exit 0
