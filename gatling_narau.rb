def main(session)
    simulation = session.attributes.get("simulation").get
    sleep_time = simulation.q.take.to_i
    print sleep_time
    sleep sleep_time
    respdata = session.attributes.get("respdata").get
    if respdata.length % sleep_time == 0
        raise "I don't like soda!"
    end
    "Ruby Red Soda!"
end

Proc.new {|session|
    main session
}
